package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.gpu.MeshPayload;
import dev.brezze.voxymobile.gpu.MeshUploadQueue;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import dev.brezze.voxymobile.scheduler.ChunkPos;
import dev.brezze.voxymobile.scheduler.MeshResult;
import dev.brezze.voxymobile.scheduler.MeshScheduler;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Connects immutable chunk snapshots -> worker meshing -> bounded render-thread upload queue. */
public final class VoxelMeshPipeline implements AutoCloseable {
    private final MeshScheduler<VoxelMeshData> scheduler;
    private final MeshUploadQueue uploads;
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private volatile boolean closed;

    public VoxelMeshPipeline(int workers, int maxQueuedJobs, MeshUploadQueue uploads) {
        this.scheduler = new MeshScheduler<>(workers, maxQueuedJobs);
        this.uploads = Objects.requireNonNull(uploads, "uploads");
    }

    /** Call on client thread with a completed immutable snapshot; no Minecraft objects are captured. */
    public boolean submit(VoxelChunkSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (closed) return false;
        ChunkPos pos = new ChunkPos(snapshot.chunkX(), snapshot.chunkZ());
        boolean queued = scheduler.submit(pos, snapshot.revision(),
            () -> GreedyVoxelMesher.build(snapshot), this::completed);
        if (queued) accepted.incrementAndGet(); else rejected.incrementAndGet();
        return queued;
    }

    private void completed(MeshResult<VoxelMeshData> result) {
        if (closed) return;
        if (!result.succeeded()) { failed.incrementAndGet(); return; }
        VoxelMeshData mesh = result.mesh();
        MeshPayload payload = new MeshPayload(result.position().x(), result.position().z(), result.revision(), mesh.vertices(), mesh.indices());
        if (!uploads.offer(payload)) rejected.incrementAndGet();
    }

    /** Invalidates in-flight work. Caller should also remove resident GPU data on render thread. */
    public void invalidate(int chunkX, int chunkZ, long revision) {
        scheduler.invalidate(new ChunkPos(chunkX, chunkZ), revision);
    }
    public void updatePlayerChunk(int chunkX, int chunkZ) { scheduler.updateCenter(chunkX, chunkZ); }
    public long acceptedCount() { return accepted.get(); }
    public long rejectedCount() { return rejected.get(); }
    public long failedCount() { return failed.get(); }
    public int queuedJobs() { return scheduler.queuedCount(); }
    @Override public void close() { closed = true; scheduler.close(); }
}
