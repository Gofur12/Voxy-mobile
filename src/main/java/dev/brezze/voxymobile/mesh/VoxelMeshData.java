package dev.brezze.voxymobile.mesh;

/** CPU mesh in chunk-local coordinates. Four floats per vertex: x, worldY, z, materialId. */
public final class VoxelMeshData {
    private final float[] vertices;
    private final int[] indices;
    private final int quadCount;

    VoxelMeshData(float[] vertices, int[] indices, int quadCount) {
        this.vertices = vertices;
        this.indices = indices;
        this.quadCount = quadCount;
    }
    public float[] vertices() { return vertices.clone(); }
    public int[] indices() { return indices.clone(); }
    public int quadCount() { return quadCount; }
    public long byteSize() { return (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES; }
}
