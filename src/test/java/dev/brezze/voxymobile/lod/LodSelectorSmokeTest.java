package dev.brezze.voxymobile.lod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LodSelectorSmokeTest {
    public static void main(String[] args) {
        double[] thresholds = {0, 64, 128, 256, 512};
        LodSelector selector = new LodSelector(4, thresholds);
        List<LodTile> tiles = selector.select(0, 0, 32, null);
        check(!tiles.isEmpty(), "selection must not be empty");
        check(containsFineTile(tiles), "near-camera area should use fine LOD");
        check(containsCoarseTile(tiles), "far area should use coarse LOD");
        checkNoOverlap(tiles);
        List<LodTile> culled = selector.select(0, 0, 32,
                (minX, minZ, maxX, maxZ) -> minX < 0);
        for (LodTile tile : culled) check(tile.chunkX * 16 < 0, "frustum hook must cull rejected tiles");
        System.out.println("LodSelectorSmokeTest: PASS");
    }

    private static boolean containsFineTile(List<LodTile> tiles) {
        for (LodTile tile : tiles) if (tile.level == 0) return true;
        return false;
    }
    private static boolean containsCoarseTile(List<LodTile> tiles) {
        for (LodTile tile : tiles) if (tile.level >= 2) return true;
        return false;
    }
    private static void checkNoOverlap(List<LodTile> tiles) {
        Set<Long> covered = new HashSet<Long>();
        for (LodTile tile : tiles) {
            for (int x = tile.chunkX; x < tile.chunkX + tile.sizeChunks; x++) {
                for (int z = tile.chunkZ; z < tile.chunkZ + tile.sizeChunks; z++) {
                    long key = (((long) x) << 32) ^ (z & 0xffffffffL);
                    check(covered.add(key), "LOD tiles must never overlap");
                }
            }
        }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
