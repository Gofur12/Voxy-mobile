package dev.brezze.voxymobile.gpu;

/** CPU-side mesh arrays waiting for upload. Positions/normals/UVs are packed by the mesher. */
public final class MeshPayload {
    private final int chunkX, chunkZ;
    private final long revision;
    private final float[] vertices;
    private final int[] indices;

    public MeshPayload(int chunkX, int chunkZ, long revision, float[] vertices, int[] indices) {
        if (revision < 0) throw new IllegalArgumentException("revision < 0");
        if (vertices == null || indices == null) throw new NullPointerException("mesh arrays");
        this.chunkX = chunkX; this.chunkZ = chunkZ; this.revision = revision;
        this.vertices = vertices.clone(); this.indices = indices.clone();
        if (indices.length % 3 != 0) throw new IllegalArgumentException("indices must be triangles");
    }
    public int chunkX() { return chunkX; }
    public int chunkZ() { return chunkZ; }
    public long revision() { return revision; }
    public float[] vertices() { return vertices.clone(); }
    public int[] indices() { return indices.clone(); }
    public long byteSize() { return (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES; }
}
