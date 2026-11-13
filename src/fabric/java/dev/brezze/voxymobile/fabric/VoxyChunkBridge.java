package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.ingest.BlockChange;
import dev.brezze.voxymobile.ingest.BlockChangeAccumulator;
import dev.brezze.voxymobile.ingest.ChunkBlockSource;
import dev.brezze.voxymobile.ingest.ChunkIngestor;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import dev.brezze.voxymobile.mesh.VoxyMobileRuntime;
import dev.brezze.voxymobile.scheduler.ChunkPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client-thread-only bridge between Fabric/Minecraft and the dependency-free ingestion core. */
public final class VoxyChunkBridge {
    private static final int DELTA_CAPACITY = 2048;
    private static final Map<Long, VoxelChunkSnapshot> SNAPSHOTS = new HashMap<>();
    // Keep a bounded history after vanilla unloads chunks so distant terrain can still be represented by LOD.
    private static final int MAX_LOD_HISTORY_CHUNKS = 128;
    private static final LinkedHashMap<Long, VoxelChunkSnapshot> LOD_HISTORY = new LinkedHashMap<>();
    private static final Map<Long, BlockChangeAccumulator> PENDING = new HashMap<>();
    private static final Map<Long, Long> REVISIONS = new HashMap<>();
    private static VoxyMobileRuntime runtime;
    /** World identity prevents stale chunk snapshots leaking across dimension/world swaps. */
    private static World activeWorld;

    /** Installed by the render integration after a GLES-backed GpuDevice is available. */
    public static void attachRuntime(VoxyMobileRuntime value) {
        runtime = value;
        if (value != null) {
            // Chunks may have loaded before the first render callback created the GLES backend.
            for (VoxelChunkSnapshot snapshot : SNAPSHOTS.values()) value.submit(snapshot);
        }
    }

    private VoxyChunkBridge() {}

    public static void onChunkLoad(World world, WorldChunk chunk) {
        requireClientWorld(world);
        ensureWorld(world);
        int minY = world.getBottomY();
        int height = world.getHeight();
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        long key = key(cx, cz);
        long revision = nextRevision(key);
        ChunkBlockSource source = new ChunkBlockSource() {
            private final BlockPos.Mutable pos = new BlockPos.Mutable();
            @Override public int minY() { return minY; }
            @Override public int height() { return height; }
            @Override public int materialId(int localX, int worldY, int localZ) {
                BlockState state = chunk.getBlockState(pos.set(cx * 16 + localX, worldY, cz * 16 + localZ));
                return Block.getRawIdFromState(state);
            }
        };
        VoxelChunkSnapshot snapshot = ChunkIngestor.ingest(cx, cz, revision, source);
        SNAPSHOTS.put(key, snapshot);
        rememberForLod(key, snapshot);
        PENDING.remove(key);
        if (runtime != null) runtime.submit(snapshot);
    }

    public static void onChunkUnload(World world, WorldChunk chunk) {
        requireClientWorld(world);
        ensureWorld(world);
        long key = key(chunk.getPos().x, chunk.getPos().z);
        SNAPSHOTS.remove(key);
        PENDING.remove(key);
        long invalidRevision = nextRevision(key); // invalidate any result carrying the previous revision
        if (runtime != null) runtime.unload(chunk.getPos().x, chunk.getPos().z, invalidRevision);
    }

    /** Called by the ClientWorld mixin after a successful client-side block-state change. */
    public static void onBlockChanged(World world, BlockPos pos, BlockState newState) {
        if (world.isClient()) {
            ensureWorld(world);
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            long key = key(cx, cz);
            VoxelChunkSnapshot snapshot = SNAPSHOTS.get(key);
            if (snapshot == null) return; // never force-load a chunk for the renderer
            BlockChangeAccumulator accumulator = PENDING.computeIfAbsent(key,
                    ignored -> new BlockChangeAccumulator(snapshot.minY(), snapshot.height(), DELTA_CAPACITY));
            accumulator.offer(new BlockChange(pos.getX() & 15, pos.getY(), pos.getZ() & 15,
                    Block.getRawIdFromState(newState)));
        }
    }

    /** Coalesced deltas are applied once per client tick, not once per individual block mutation. */
    public static void flushPendingChanges() {
        Iterator<Map.Entry<Long, BlockChangeAccumulator>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, BlockChangeAccumulator> entry = it.next();
            VoxelChunkSnapshot current = SNAPSHOTS.get(entry.getKey());
            if (current == null || entry.getValue().pendingCount() == 0) {
                it.remove();
                continue;
            }
            long revision = nextRevision(entry.getKey());
            VoxelChunkSnapshot updated = entry.getValue().applyTo(current, revision);
            SNAPSHOTS.put(entry.getKey(), updated);
            rememberForLod(entry.getKey(), updated);
            if (runtime != null) runtime.submit(updated);
            it.remove();
        }
    }

    /** Snapshot lookup is intentionally client-thread-only until a dedicated concurrent handoff is added. */
    public static VoxelChunkSnapshot getSnapshot(int chunkX, int chunkZ) {
        return SNAPSHOTS.get(key(chunkX, chunkZ));
    }

    /** Client-thread snapshot of loaded chunk coordinates for the render-frame bridge. */
    /** Immutable point-in-time list of loaded and recently unloaded snapshots for distant LOD. */
    public static List<VoxelChunkSnapshot> getLodSnapshots() {
        return new ArrayList<>(LOD_HISTORY.values());
    }

    public static List<ChunkPos> getLoadedChunkPositions() {
        List<ChunkPos> result = new ArrayList<>(SNAPSHOTS.size());
        for (long packed : SNAPSHOTS.keySet()) result.add(new ChunkPos((int) (packed >> 32), (int) packed));
        return result;
    }

    public static void clear() {
        SNAPSHOTS.clear();
        LOD_HISTORY.clear();
        PENDING.clear();
        REVISIONS.clear();
        activeWorld = null;
        runtime = null;
    }

    /** Drop all old-world data and invalidate its in-flight meshes before accepting new chunks. */
    private static void ensureWorld(World world) {
        if (activeWorld == world) return;
        if (runtime != null) {
            for (VoxelChunkSnapshot snapshot : SNAPSHOTS.values()) {
                long key = key(snapshot.chunkX(), snapshot.chunkZ());
                runtime.unload(snapshot.chunkX(), snapshot.chunkZ(), nextRevision(key));
            }
        }
        SNAPSHOTS.clear();
        LOD_HISTORY.clear();
        PENDING.clear();
        // Keep revisions monotonic while the runtime remains attached. Reusing revision 1
        // after a dimension swap could let a stale worker result overwrite a new chunk mesh.
        activeWorld = world;
    }

    private static void rememberForLod(long key, VoxelChunkSnapshot snapshot) {
        LOD_HISTORY.remove(key);
        LOD_HISTORY.put(key, snapshot);
        while (LOD_HISTORY.size() > MAX_LOD_HISTORY_CHUNKS) {
            Long eldest = LOD_HISTORY.keySet().iterator().next();
            LOD_HISTORY.remove(eldest);
        }
    }

    private static long nextRevision(long key) {
        long revision = REVISIONS.getOrDefault(key, 0L) + 1L;
        REVISIONS.put(key, revision);
        return revision;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static void requireClientWorld(World world) {
        if (!world.isClient()) throw new IllegalArgumentException("VoxyMobile chunk bridge is client-only");
    }
}
