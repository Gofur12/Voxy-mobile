package dev.brezze.voxymobile.lod;

/** CPU-side indexed terrain mesh. Positions are XYZ triples in block units. */
public final class TerrainLodMesh {
    private final float[] positions;
    private final int[] indices;
    private final int surfaceVertexCount;

    TerrainLodMesh(float[] positions, int[] indices, int surfaceVertexCount) {
        this.positions = positions;
        this.indices = indices;
        this.surfaceVertexCount = surfaceVertexCount;
    }

    public float[] positions() { return positions; }
    public int[] indices() { return indices; }
    public int vertexCount() { return positions.length / 3; }
    public int indexCount() { return indices.length; }
    public int surfaceVertexCount() { return surfaceVertexCount; }
    public int byteSize() { return positions.length * Float.BYTES + indices.length * Integer.BYTES; }
}
