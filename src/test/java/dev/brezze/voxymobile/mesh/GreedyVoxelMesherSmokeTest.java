package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.ingest.ChunkBlockSource;
import dev.brezze.voxymobile.ingest.ChunkIngestor;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;

public final class GreedyVoxelMesherSmokeTest {
    public static void main(String[] args) {
        ChunkBlockSource oneBlock = new ChunkBlockSource() {
            public int minY() { return -64; }
            public int height() { return 384; }
            public int materialId(int x, int y, int z) { return x==0 && y==0 && z==0 ? 7 : 0; }
        };
        VoxelChunkSnapshot snapshot = ChunkIngestor.ingest(2, -3, 1, oneBlock);
        VoxelMeshData mesh = GreedyVoxelMesher.build(snapshot);
        if (mesh.quadCount()!=6) throw new AssertionError("single isolated block should have 6 faces, got "+mesh.quadCount());
        if (mesh.vertices().length != 24*4) throw new AssertionError("expected 24 vertices x 4 floats");
        if (mesh.indices().length != 36) throw new AssertionError("expected 36 indices");

        ChunkBlockSource solid2 = new ChunkBlockSource() {
            public int minY(){return 0;} public int height(){return 2;}
            public int materialId(int x,int y,int z){return x<2 && y<2 && z<2 ? 3 : 0;}
        };
        VoxelMeshData merged = GreedyVoxelMesher.build(ChunkIngestor.ingest(0,0,1,solid2));
        if (merged.quadCount()!=6) throw new AssertionError("2x2x2 same-material cube should greedy merge to 6 quads, got "+merged.quadCount());
        System.out.println("GreedyVoxelMesherSmokeTest: PASS");
    }
}
