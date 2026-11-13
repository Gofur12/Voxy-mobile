package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.gpu.GpuMesh;
import dev.brezze.voxymobile.gpu.GpuDevice;
import dev.brezze.voxymobile.gpu.gles.VoxelTerrainRenderer;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import dev.brezze.voxymobile.lod.LodSelector;
import dev.brezze.voxymobile.lod.LodTile;
import dev.brezze.voxymobile.lod.TerrainLodMesh;
import dev.brezze.voxymobile.lod.TerrainLodMesher;
import dev.brezze.voxymobile.lod.TileVisibility;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * First integrated distant-terrain path. It retains a bounded history of visited chunk snapshots,
 * selects non-overlapping quadtree tiles, builds sampled heightfields, and draws them beyond the
 * vanilla view-distance boundary. All GL operations happen on the render thread.
 */
public final class LodHeightfieldRenderer implements AutoCloseable {
    private static final int MAX_LOD_LEVEL = 2;
    private static final int MAX_RADIUS_CHUNKS = 32;
    private final GpuDevice gpu;
    private final VoxelTerrainRenderer renderer;
    private final LodSelector selector = new LodSelector(MAX_LOD_LEVEL, new double[]{0.0, 112.0, 224.0});
    private final Map<Long, CachedTile> cache = new HashMap<>();
    private Thread renderThread;
    private boolean closed;

    public LodHeightfieldRenderer(GpuDevice gpu, VoxelTerrainRenderer renderer) {
        if (gpu == null || renderer == null) throw new NullPointerException();
        this.gpu = gpu;
        this.renderer = renderer;
    }

    public int render(List<VoxelChunkSnapshot> source, float[] viewProjection,
                      double cameraX, double cameraY, double cameraZ) {
        checkThread();
        if (closed) return 0;
        if (source == null || viewProjection == null) throw new NullPointerException();
        Map<Long, VoxelChunkSnapshot> chunks = new HashMap<>(source.size() * 2);
        for (VoxelChunkSnapshot snapshot : source) chunks.put(key(snapshot.chunkX(), snapshot.chunkZ()), snapshot);

        int vanillaDistance = 8;
        try {
            vanillaDistance = Math.max(2, MinecraftClient.getInstance().options.getViewDistance().getValue());
        } catch (RuntimeException ignored) {
            // Keep a conservative fallback for launchers exposing a partial options facade.
        }
        final double minLodDistance = vanillaDistance * 16.0;
        TileVisibility outsideVanilla = (minX, minZ, maxX, maxZ) -> {
            double dx = distanceToInterval(cameraX, minX, maxX);
            double dz = distanceToInterval(cameraZ, minZ, maxZ);
            return dx * dx + dz * dz >= minLodDistance * minLodDistance;
        };
        List<LodTile> selected = selector.select(cameraX, cameraZ, MAX_RADIUS_CHUNKS, outsideVanilla);
        Set<Long> active = new HashSet<>();
        int drawn = 0;
        for (LodTile tile : selected) {
            drawn += drawWithFallback(tile, chunks, viewProjection, cameraX, cameraY, cameraZ, active);
        }
        // Release tiles that have moved out of the selected LOD ring or lost source data.
        List<Long> stale = new ArrayList<>();
        for (Map.Entry<Long, CachedTile> item : cache.entrySet()) {
            if (!active.contains(item.getKey())) stale.add(item.getKey());
        }
        for (Long key : stale) {
            CachedTile old = cache.remove(key);
            if (old != null) gpu.deleteMesh(old.mesh.handle());
        }
        return drawn;
    }


    private int drawWithFallback(LodTile tile, Map<Long, VoxelChunkSnapshot> chunks,
                                 float[] viewProjection, double cameraX, double cameraY, double cameraZ,
                                 Set<Long> active) {
        if (!hasCompleteTile(tile, chunks)) {
            if (tile.level == 0) return 0;
            int childLevel = tile.level - 1;
            int half = tile.sizeChunks >> 1;
            int drawn = 0;
            drawn += drawWithFallback(new LodTile(tile.chunkX, tile.chunkZ, childLevel), chunks,
                    viewProjection, cameraX, cameraY, cameraZ, active);
            drawn += drawWithFallback(new LodTile(tile.chunkX + half, tile.chunkZ, childLevel), chunks,
                    viewProjection, cameraX, cameraY, cameraZ, active);
            drawn += drawWithFallback(new LodTile(tile.chunkX, tile.chunkZ + half, childLevel), chunks,
                    viewProjection, cameraX, cameraY, cameraZ, active);
            drawn += drawWithFallback(new LodTile(tile.chunkX + half, tile.chunkZ + half, childLevel), chunks,
                    viewProjection, cameraX, cameraY, cameraZ, active);
            return drawn;
        }
        long signature = signature(tile, chunks);
        long cacheKey = tile.key();
        active.add(cacheKey);
        CachedTile entry = cache.get(cacheKey);
        if (entry == null || entry.signature != signature) {
            if (entry != null) gpu.deleteMesh(entry.mesh.handle());
            GpuMesh mesh = build(tile, signature, chunks);
            if (mesh == null) {
                cache.remove(cacheKey);
                return 0;
            }
            entry = new CachedTile(signature, mesh);
            cache.put(cacheKey, entry);
        }
        renderer.draw(entry.mesh, viewProjection, cameraX, cameraY, cameraZ);
        return 1;
    }

    private GpuMesh build(LodTile tile, long signature, Map<Long, VoxelChunkSnapshot> chunks) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int z = 0; z < tile.sizeChunks; z++) for (int x = 0; x < tile.sizeChunks; x++) {
            VoxelChunkSnapshot c = chunks.get(key(tile.chunkX + x, tile.chunkZ + z));
            minY = Math.min(minY, c.minY());
            maxY = Math.max(maxY, c.maxYExclusive());
        }
        final int floorY = minY;
        final int tileStartX = tile.chunkX * 16;
        final int tileStartZ = tile.chunkZ * 16;
        final int tileEndX = tileStartX + tile.sizeChunks * 16 - 1;
        final int tileEndZ = tileStartZ + tile.sizeChunks * 16 - 1;
        TerrainLodMesh lod = TerrainLodMesher.build(tile, minY, maxY,
                4 << tile.level, 10.0f, (worldX, worldZ) -> sampleHeight(chunks,
                        Math.min(worldX, tileEndX), Math.min(worldZ, tileEndZ), floorY));
        float[] positions = lod.positions();
        float[] vertices = new float[(positions.length / 3) * 4];
        float originX = tile.chunkX * 16.0f;
        float originZ = tile.chunkZ * 16.0f;
        for (int i = 0, v = 0; i < positions.length; i += 3) {
            vertices[v++] = positions[i] - originX;
            vertices[v++] = positions[i + 1];
            vertices[v++] = positions[i + 2] - originZ;
            vertices[v++] = 1.0f; // neutral terrain material; no random per-block color hashing
        }
        int[] indices = lod.indices();
        int handle = gpu.createMesh(vertices, indices);
        return new GpuMesh(handle, tile.chunkX, tile.chunkZ, signature,
                (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES);
    }

    private static float sampleHeight(Map<Long, VoxelChunkSnapshot> chunks, int worldX, int worldZ, int fallbackY) {
        int cx = Math.floorDiv(worldX, 16), cz = Math.floorDiv(worldZ, 16);
        VoxelChunkSnapshot chunk = chunks.get(key(cx, cz));
        if (chunk == null) return fallbackY;
        int lx = Math.floorMod(worldX, 16), lz = Math.floorMod(worldZ, 16);
        for (int y = chunk.maxYExclusive() - 1; y >= chunk.minY(); y--) {
            if (chunk.materialId(lx, y, lz) != 0) return y + 1.0f;
        }
        return chunk.minY();
    }

    private static boolean hasCompleteTile(LodTile tile, Map<Long, VoxelChunkSnapshot> chunks) {
        for (int z = 0; z < tile.sizeChunks; z++) for (int x = 0; x < tile.sizeChunks; x++)
            if (!chunks.containsKey(key(tile.chunkX + x, tile.chunkZ + z))) return false;
        return true;
    }

    private static long signature(LodTile tile, Map<Long, VoxelChunkSnapshot> chunks) {
        long value = 0xcbf29ce484222325L;
        for (int z = 0; z < tile.sizeChunks; z++) for (int x = 0; x < tile.sizeChunks; x++) {
            VoxelChunkSnapshot c = chunks.get(key(tile.chunkX + x, tile.chunkZ + z));
            value ^= c.revision() + (((long) c.chunkX()) << 32) + c.chunkZ();
            value *= 0x100000001b3L;
        }
        return value;
    }

    private static double distanceToInterval(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    @Override public void close() {
        checkThread();
        if (closed) return;
        closed = true;
        for (CachedTile tile : cache.values()) gpu.deleteMesh(tile.mesh.handle());
        cache.clear();
    }

    private void checkThread() {
        Thread now = Thread.currentThread();
        if (renderThread == null) renderThread = now;
        else if (renderThread != now) throw new IllegalStateException("LOD renderer used outside render thread");
    }

    private static final class CachedTile {
        final long signature;
        final GpuMesh mesh;
        CachedTile(long signature, GpuMesh mesh) { this.signature = signature; this.mesh = mesh; }
    }
}
