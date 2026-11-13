package dev.brezze.voxymobile.lod;

import java.util.Arrays;

/** Builds a sampled heightfield surface and downward edge skirts for a selected LOD tile.
 * Skirts conceal T-junction cracks; they are a conservative seam solution, not exact stitching.
 */
public final class TerrainLodMesher {
    private TerrainLodMesher() {}

    public static TerrainLodMesh build(LodTile tile, int minY, int maxY,
                                       int sampleStepBlocks, float skirtDepth,
                                       HeightSampler sampler) {
        if (tile == null || sampler == null) throw new NullPointerException();
        if (sampleStepBlocks <= 0 || skirtDepth < 0 || !Float.isFinite(skirtDepth))
            throw new IllegalArgumentException("invalid step or skirt depth");
        if (maxY <= minY) throw new IllegalArgumentException("maxY must exceed minY");
        int span = Math.multiplyExact(tile.sizeChunks, 16);
        if (span % sampleStepBlocks != 0)
            throw new IllegalArgumentException("sample step must divide tile width");
        int cells = span / sampleStepBlocks;
        int side = cells + 1;
        int startX = Math.multiplyExact(tile.chunkX, 16);
        int startZ = Math.multiplyExact(tile.chunkZ, 16);
        FloatBuilder p = new FloatBuilder(side * side * 3 + (cells * 4) * 6);
        IntBuilder ix = new IntBuilder(cells * cells * 6 + cells * 4 * 6);

        for (int z = 0; z <= cells; z++) {
            for (int x = 0; x <= cells; x++) {
                int wx = startX + x * sampleStepBlocks;
                int wz = startZ + z * sampleStepBlocks;
                float y = sampler.heightAt(wx, wz);
                if (!Float.isFinite(y)) throw new IllegalArgumentException("non-finite terrain height");
                y = Math.max(minY, Math.min(maxY, y));
                p.add(wx, y, wz);
            }
        }
        int surfaceVertices = side * side;
        for (int z = 0; z < cells; z++) {
            for (int x = 0; x < cells; x++) {
                int a = z * side + x;
                int b = a + 1;
                int c = a + side;
                int d = c + 1;
                // Consistent upward-facing winding when viewed from above.
                ix.add(a, c, b, b, c, d);
            }
        }
        if (skirtDepth > 0) {
            addSkirt(p, ix, edge(side, cells, 0), skirtDepth);          // north
            addSkirt(p, ix, edge(side, cells, 1), skirtDepth);          // east
            addSkirt(p, ix, edge(side, cells, 2), skirtDepth);          // south
            addSkirt(p, ix, edge(side, cells, 3), skirtDepth);          // west
        }
        return new TerrainLodMesh(p.toArray(), ix.toArray(), surfaceVertices);
    }

    private static int[] edge(int side, int cells, int direction) {
        int[] out = new int[cells + 1];
        for (int i = 0; i <= cells; i++) {
            switch (direction) {
                case 0: out[i] = i; break;
                case 1: out[i] = i * side + cells; break;
                case 2: out[i] = cells * side + (cells - i); break;
                default: out[i] = (cells - i) * side;
            }
        }
        return out;
    }

    private static void addSkirt(FloatBuilder p, IntBuilder ix, int[] edge, float depth) {
        int previousTop = -1, previousBottom = -1;
        for (int top : edge) {
            int bottom = p.size() / 3;
            float x = p.get(top * 3), y = p.get(top * 3 + 1), z = p.get(top * 3 + 2);
            p.add(x, y - depth, z);
            if (previousTop >= 0) ix.add(previousTop, previousBottom, top, top, previousBottom, bottom);
            previousTop = top;
            previousBottom = bottom;
        }
    }

    private static final class FloatBuilder {
        private float[] a; private int n;
        FloatBuilder(int capacity) { a = new float[Math.max(16, capacity)]; }
        void add(float x, float y, float z) { ensure(3); a[n++] = x; a[n++] = y; a[n++] = z; }
        float get(int i) { return a[i]; }
        int size() { return n; }
        float[] toArray() { return Arrays.copyOf(a, n); }
        private void ensure(int count) { if (n + count > a.length) a = Arrays.copyOf(a, Math.max(n + count, a.length * 2)); }
    }
    private static final class IntBuilder {
        private int[] a; private int n;
        IntBuilder(int capacity) { a = new int[Math.max(16, capacity)]; }
        void add(int... values) { if (n + values.length > a.length) a = Arrays.copyOf(a, Math.max(n + values.length, a.length * 2)); for (int v : values) a[n++] = v; }
        int[] toArray() { return Arrays.copyOf(a, n); }
    }
}
