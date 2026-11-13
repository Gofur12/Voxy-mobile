package dev.brezze.voxymobile.gpu;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Bounded CPU->GPU handoff. drain() must be called only from the render thread. */
public final class MeshUploadQueue {
    private final ArrayDeque<MeshPayload> queue = new ArrayDeque<>();
    private final Map<Long, Long> newestRevision = new HashMap<>();
    private final Map<Long, GpuMesh> resident = new HashMap<>();
    private final int maxQueued;
    private final long maxQueuedBytes;
    private long queuedBytes;
    private Thread renderThread;

    public MeshUploadQueue(int maxQueued, long maxQueuedBytes) {
        if (maxQueued < 1 || maxQueuedBytes < 1) throw new IllegalArgumentException("limits must be positive");
        this.maxQueued = maxQueued; this.maxQueuedBytes = maxQueuedBytes;
    }
    private static long key(int x, int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }

    /** Replaces queued work for the same chunk; rejects stale revisions and over-budget payloads. */
    public synchronized boolean offer(MeshPayload payload) {
        long k = key(payload.chunkX(), payload.chunkZ());
        long newest = newestRevision.getOrDefault(k, -1L);
        if (payload.revision() <= newest || payload.byteSize() > maxQueuedBytes) return false;
        newestRevision.put(k, payload.revision());
        for (var it = queue.iterator(); it.hasNext();) {
            MeshPayload old = it.next();
            if (old.chunkX() == payload.chunkX() && old.chunkZ() == payload.chunkZ()) {
                queuedBytes -= old.byteSize(); it.remove(); break;
            }
        }
        while (queue.size() >= maxQueued || queuedBytes + payload.byteSize() > maxQueuedBytes) {
            MeshPayload dropped = queue.pollFirst();
            if (dropped == null) return false;
            queuedBytes -= dropped.byteSize();
        }
        queue.addLast(payload); queuedBytes += payload.byteSize();
        return true;
    }

    /** Uploads at most maxUploads and maxBytes this frame. A single first item may exceed maxBytes. */
    public int drain(GpuDevice device, int maxUploads, long maxBytes) {
        if (maxUploads < 1 || maxBytes < 1) return 0;
        bindRenderThread();
        int count = 0; long used = 0;
        while (count < maxUploads) {
            MeshPayload p;
            synchronized (this) {
                p = queue.peekFirst();
                if (p == null) break;
                if (count > 0 && used + p.byteSize() > maxBytes) break;
                queue.removeFirst(); queuedBytes -= p.byteSize();
                long current = newestRevision.getOrDefault(key(p.chunkX(), p.chunkZ()), -1L);
                if (p.revision() != current) continue;
            }
            int handle = device.createMesh(p.vertices(), p.indices());
            GpuMesh replacement = new GpuMesh(handle, p.chunkX(), p.chunkZ(), p.revision(), p.byteSize());
            GpuMesh previous;
            synchronized (this) { previous = resident.put(key(p.chunkX(), p.chunkZ()), replacement); }
            if (previous != null) device.deleteMesh(previous.handle());
            used += p.byteSize(); count++;
        }
        return count;
    }

    /** Invalidates queued/in-flight payload revisions without making an OpenGL call. */
    public synchronized void invalidate(int chunkX, int chunkZ, long minimumRevision) {
        long k = key(chunkX, chunkZ);
        newestRevision.merge(k, minimumRevision, Math::max);
        for (var it = queue.iterator(); it.hasNext();) {
            MeshPayload p = it.next();
            if (p.chunkX() == chunkX && p.chunkZ() == chunkZ && p.revision() < minimumRevision) {
                queuedBytes -= p.byteSize();
                it.remove();
            }
        }
    }

    public void remove(int chunkX, int chunkZ, GpuDevice device) {
        bindRenderThread();
        GpuMesh old;
        synchronized (this) {
            long k = key(chunkX, chunkZ); newestRevision.remove(k);
            for (var it = queue.iterator(); it.hasNext();) {
                MeshPayload p = it.next();
                if (p.chunkX() == chunkX && p.chunkZ() == chunkZ) { queuedBytes -= p.byteSize(); it.remove(); }
            }
            old = resident.remove(k);
        }
        if (old != null) device.deleteMesh(old.handle());
    }
    /** Deletes all resident GPU meshes and drops queued CPU payloads. Render-thread only. */
    public void clear(GpuDevice device) {
        bindRenderThread();
        GpuMesh[] old;
        synchronized (this) {
            old = resident.values().toArray(new GpuMesh[0]);
            resident.clear();
            queue.clear();
            newestRevision.clear();
            queuedBytes = 0;
        }
        for (GpuMesh mesh : old) device.deleteMesh(mesh.handle());
    }

    public synchronized GpuMesh get(int x, int z) { return resident.get(key(x, z)); }
    public synchronized int queuedCount() { return queue.size(); }
    public synchronized long queuedBytes() { return queuedBytes; }
    private void bindRenderThread() {
        Thread now = Thread.currentThread();
        if (renderThread == null) renderThread = now;
        else if (renderThread != now) throw new IllegalStateException("GPU operation called outside render thread");
    }
}
