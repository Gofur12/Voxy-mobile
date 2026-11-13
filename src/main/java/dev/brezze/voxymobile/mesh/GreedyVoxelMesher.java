package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import java.util.Arrays;

/** Greedy exposed-face mesher. Material ID 0 is treated as air. No Minecraft or GL calls. */
public final class GreedyVoxelMesher {
    private GreedyVoxelMesher() {}

    public static VoxelMeshData build(VoxelChunkSnapshot chunk) {
        if (chunk == null) throw new NullPointerException("chunk");
        Builder out = new Builder();
        // Axes: 0=X, 1=Y, 2=Z. The 2-D mask is merged by identical material and face direction.
        int[] dims = {16, chunk.height(), 16};
        int[] p = new int[3];
        int[] q = new int[3];
        for (int d = 0; d < 3; d++) {
            int u = (d + 1) % 3;
            int v = (d + 2) % 3;
            Arrays.fill(q, 0);
            q[d] = 1;
            int[] mask = new int[dims[u] * dims[v]];
            for (p[d] = -1; p[d] < dims[d]; p[d]++) {
                int n = 0;
                for (p[v] = 0; p[v] < dims[v]; p[v]++) {
                    for (p[u] = 0; p[u] < dims[u]; p[u]++) {
                        int a = sample(chunk, p[0], p[1], p[2], dims);
                        int b = sample(chunk, p[0] + q[0], p[1] + q[1], p[2] + q[2], dims);
                        // Positive material = face toward +axis; negative = face toward -axis.
                        mask[n++] = a != 0 && b == 0 ? a : (a == 0 && b != 0 ? -b : 0);
                    }
                }
                n = 0;
                for (int j = 0; j < dims[v]; j++) {
                    for (int i = 0; i < dims[u];) {
                        int m = mask[n];
                        if (m == 0) { i++; n++; continue; }
                        int w = 1;
                        while (i + w < dims[u] && mask[n + w] == m) w++;
                        int h = 1;
                        boolean stop = false;
                        while (j + h < dims[v] && !stop) {
                            for (int k = 0; k < w; k++) {
                                if (mask[n + k + h * dims[u]] != m) { stop = true; break; }
                            }
                            if (!stop) h++;
                        }
                        p[u] = i; p[v] = j;
                        int[] du = new int[3]; int[] dv = new int[3];
                        du[u] = w; dv[v] = h;
                        int[] base = p.clone(); base[d]++;
                        out.quad(base, du, dv, chunk.minY(), Math.abs(m), m > 0);
                        for (int y = 0; y < h; y++)
                            for (int x = 0; x < w; x++) mask[n + x + y * dims[u]] = 0;
                        i += w; n += w;
                    }
                }
            }
        }
        return out.finish();
    }

    private static int sample(VoxelChunkSnapshot c, int x, int y, int z, int[] dims) {
        if (x < 0 || x >= dims[0] || y < 0 || y >= dims[1] || z < 0 || z >= dims[2]) return 0;
        return c.materialId(x, c.minY() + y, z);
    }

    private static final class Builder {
        float[] vertices = new float[256]; int vertexFloats;
        int[] indices = new int[384]; int indexCount; int quads;
        void quad(int[] base, int[] du, int[] dv, int minY, int material, boolean positive) {
            int x=base[0], y=base[1]+minY, z=base[2];
            int[][] corners = {
                {x,y,z}, {x+du[0],y+du[1],z+du[2]},
                {x+du[0]+dv[0],y+du[1]+dv[1],z+du[2]+dv[2]},
                {x+dv[0],y+dv[1],z+dv[2]}
            };
            int start=vertexFloats/4;
            for (int[] c : corners) { put(c[0]); put(c[1]); put(c[2]); put(material); }
            ensureIndices(6);
            if (positive) { indices[indexCount++]=start; indices[indexCount++]=start+1; indices[indexCount++]=start+2; indices[indexCount++]=start; indices[indexCount++]=start+2; indices[indexCount++]=start+3; }
            else { indices[indexCount++]=start; indices[indexCount++]=start+2; indices[indexCount++]=start+1; indices[indexCount++]=start; indices[indexCount++]=start+3; indices[indexCount++]=start+2; }
            quads++;
        }
        void put(float f) { if(vertexFloats==vertices.length) vertices=Arrays.copyOf(vertices,vertices.length*2); vertices[vertexFloats++]=f; }
        void ensureIndices(int n) { if(indexCount+n>indices.length) indices=Arrays.copyOf(indices,Math.max(indices.length*2,indexCount+n)); }
        VoxelMeshData finish() { return new VoxelMeshData(Arrays.copyOf(vertices,vertexFloats),Arrays.copyOf(indices,indexCount),quads); }
    }
}
