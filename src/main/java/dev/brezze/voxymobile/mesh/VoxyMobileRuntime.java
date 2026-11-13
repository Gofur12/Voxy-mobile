package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.android.AdaptiveUploadBudget;
import dev.brezze.voxymobile.android.AndroidPerformanceProfile;
import dev.brezze.voxymobile.android.ChunkDistanceCuller;
import dev.brezze.voxymobile.gpu.GpuDevice;
import dev.brezze.voxymobile.gpu.GpuMesh;
import dev.brezze.voxymobile.gpu.MeshUploadQueue;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Coordinates the complete CPU meshing -> bounded upload -> resident GPU mesh lifecycle.
 * submit/invalidate may be called on the client thread; beginFrame/getResident are render-thread calls.
 */
public final class VoxyMobileRuntime implements AutoCloseable {
    private final MeshUploadQueue uploads;
    private final VoxelMeshPipeline pipeline;
    private final GpuDevice gpu;
    private final AdaptiveUploadBudget uploadBudget;
    private final ChunkDistanceCuller culler;
    private final Set<Long> pendingGpuRemovals = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public VoxyMobileRuntime(GpuDevice gpu, AndroidPerformanceProfile profile,
                             int viewDistanceChunks, int maxUploadQueue, long maxUploadQueueBytes) {
        if (gpu == null || profile == null) throw new NullPointerException();
        this.gpu = gpu;
        this.uploads = new MeshUploadQueue(maxUploadQueue, maxUploadQueueBytes);
        this.pipeline = new VoxelMeshPipeline(profile.workerCount, profile.maxQueuedJobs, uploads);
        this.uploadBudget = new AdaptiveUploadBudget(1, profile.maxUploadsPerFrame,
                Math.min(128L << 10, profile.uploadBytesPerFrame), profile.uploadBytesPerFrame);
        this.culler = new ChunkDistanceCuller(viewDistanceChunks);
    }

    /** Call on the client thread after a chunk snapshot has been created/updated. */
    public boolean submit(VoxelChunkSnapshot snapshot) {
        if (closed) return false;
        pendingGpuRemovals.remove(key(snapshot.chunkX(), snapshot.chunkZ()));
        return pipeline.submit(snapshot);
    }

    /** Invalidates worker and queued upload results. Actual GL deletion is deferred to beginFrame. */
    public void unload(int chunkX, int chunkZ, long invalidRevision) {
        if (closed) return;
        pipeline.invalidate(chunkX, chunkZ, invalidRevision);
        uploads.invalidate(chunkX, chunkZ, invalidRevision);
        pendingGpuRemovals.add(key(chunkX, chunkZ));
    }

    public void updatePlayerChunk(int chunkX, int chunkZ) {
        pipeline.updatePlayerChunk(chunkX, chunkZ);
    }

    /** Call once per frame on render thread; drains deferred deletes before bounded uploads. */
    public int beginFrame(double previousFrameMillis) {
        if (closed) return 0;
        uploadBudget.observeFrameMillis(previousFrameMillis);
        for (Long packed : pendingGpuRemovals.toArray(new Long[0])) {
            int x = (int) (packed >> 32), z = (int) (long) packed;
            uploads.remove(x, z, gpu);
            pendingGpuRemovals.remove(packed);
        }
        return uploads.drain(gpu, uploadBudget.maxUploads(), uploadBudget.maxBytes());
    }

    /** Returns null when outside view distance or not uploaded yet. Render-thread only via caller discipline. */
    public GpuMesh visibleMesh(int chunkX, int chunkZ, double cameraBlockX, double cameraBlockZ) {
        if (!culler.isVisible(chunkX, chunkZ, cameraBlockX, cameraBlockZ)) return null;
        return uploads.get(chunkX, chunkZ);
    }

    /** Stops CPU workers and releases every resident GPU mesh. Must be called on the GL render thread. */
    public void close() {
        if (closed) return;
        closed = true;
        pipeline.close();
        uploads.clear(gpu);
        pendingGpuRemovals.clear();
    }

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
}
