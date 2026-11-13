package dev.brezze.voxymobile.lod;

public final class TerrainLodMesherSmokeTest {
    public static void main(String[] args) {
        LodTile tile = new LodTile(2, -1, 0);
        TerrainLodMesh mesh = TerrainLodMesher.build(tile, -64, 319, 2, 4f,
                (x, z) -> 100f + (x % 4) * 0.25f);
        check(mesh.surfaceVertexCount() == 81, "9x9 surface grid expected");
        check(mesh.indexCount() == 576, "surface plus four skirts expected");
        check(mesh.vertexCount() == 81 + 4 * 9, "skirt vertices expected");
        int[] indices = mesh.indices();
        for (int index : indices) check(index >= 0 && index < mesh.vertexCount(), "index out of range");
        float[] p = mesh.positions();
        for (float v : p) check(Float.isFinite(v), "all vertex coordinates must be finite");
        check(mesh.byteSize() == p.length * 4 + indices.length * 4, "byte accounting");
        boolean rejected = false;
        try { TerrainLodMesher.build(tile, -64, 319, 3, 4f, (x, z) -> 0); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "non-dividing sample step must be rejected");
        System.out.println("TerrainLodMesherSmokeTest: PASS");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
