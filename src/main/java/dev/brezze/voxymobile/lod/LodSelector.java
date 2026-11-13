package dev.brezze.voxymobile.lod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Selects a non-overlapping quadtree cover around the camera. CPU-only; no terrain data is generated here. */
public final class LodSelector {
    private final int maxLevel;
    private final double[] splitDistanceBlocks;

    /** splitDistanceBlocks[level] is the distance below which a tile at that level is subdivided. */
    public LodSelector(int maxLevel, double[] splitDistanceBlocks) {
        if (maxLevel < 0 || maxLevel > 10) throw new IllegalArgumentException("maxLevel must be 0..10");
        if (splitDistanceBlocks == null || splitDistanceBlocks.length <= maxLevel)
            throw new IllegalArgumentException("one split distance is required for every level");
        this.maxLevel = maxLevel;
        this.splitDistanceBlocks = splitDistanceBlocks.clone();
        for (int i = 1; i <= maxLevel; i++) {
            if (this.splitDistanceBlocks[i] < 0 || Double.isNaN(this.splitDistanceBlocks[i]))
                throw new IllegalArgumentException("split distances must be non-negative");
        }
    }

    public List<LodTile> select(double cameraBlockX, double cameraBlockZ, int viewRadiusChunks,
                                TileVisibility visibility) {
        if (viewRadiusChunks < 1) throw new IllegalArgumentException("view radius must be positive");
        int cameraChunkX = floorChunk(cameraBlockX);
        int cameraChunkZ = floorChunk(cameraBlockZ);
        int rootSize = 1 << maxLevel;
        int minX = Math.floorDiv(cameraChunkX - viewRadiusChunks, rootSize) * rootSize;
        int maxX = Math.floorDiv(cameraChunkX + viewRadiusChunks, rootSize) * rootSize;
        int minZ = Math.floorDiv(cameraChunkZ - viewRadiusChunks, rootSize) * rootSize;
        int maxZ = Math.floorDiv(cameraChunkZ + viewRadiusChunks, rootSize) * rootSize;
        double radiusBlocks = viewRadiusChunks * 16.0;
        double radiusSq = radiusBlocks * radiusBlocks;
        List<LodTile> out = new ArrayList<LodTile>();
        for (int x = minX; x <= maxX; x += rootSize) {
            for (int z = minZ; z <= maxZ; z += rootSize) {
                visit(x, z, maxLevel, cameraBlockX, cameraBlockZ, radiusSq, visibility, out);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private void visit(int x, int z, int level, double cx, double cz, double radiusSq,
                       TileVisibility visibility, List<LodTile> out) {
        int size = 1 << level;
        int minBX = x * 16;
        int minBZ = z * 16;
        int maxBX = minBX + size * 16;
        int maxBZ = minBZ + size * 16;
        double dx = distanceToInterval(cx, minBX, maxBX);
        double dz = distanceToInterval(cz, minBZ, maxBZ);
        if (dx * dx + dz * dz > radiusSq) return;
        if (visibility != null && !visibility.isVisible(minBX, minBZ, maxBX, maxBZ)) return;

        double centerX = (minBX + maxBX) * 0.5;
        double centerZ = (minBZ + maxBZ) * 0.5;
        double centerDx = centerX - cx;
        double centerDz = centerZ - cz;
        boolean split = level > 0 && centerDx * centerDx + centerDz * centerDz
                < splitDistanceBlocks[level] * splitDistanceBlocks[level];
        if (!split) {
            out.add(new LodTile(x, z, level));
            return;
        }
        int half = size >> 1;
        int childLevel = level - 1;
        visit(x, z, childLevel, cx, cz, radiusSq, visibility, out);
        visit(x + half, z, childLevel, cx, cz, radiusSq, visibility, out);
        visit(x, z + half, childLevel, cx, cz, radiusSq, visibility, out);
        visit(x + half, z + half, childLevel, cx, cz, radiusSq, visibility, out);
    }

    private static double distanceToInterval(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0;
    }

    private static int floorChunk(double block) {
        return (int) Math.floor(block / 16.0);
    }
}
