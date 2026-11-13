package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.gpu.MeshUploadQueue;
import dev.brezze.voxymobile.ingest.ChunkBlockSource;
import dev.brezze.voxymobile.ingest.ChunkIngestor;

public final class VoxelMeshPipelineSmokeTest {
    public static void main(String[] args) throws Exception {
        MeshUploadQueue uploads = new MeshUploadQueue(8, 1024 * 1024);
        try (VoxelMeshPipeline pipeline = new VoxelMeshPipeline(1, 8, uploads)) {
            ChunkBlockSource source = new ChunkBlockSource() {
                public int minY() { return 0; }
                public int height() { return 1; }
                public int materialId(int x, int y, int z) { return x==0 && y==0 && z==0 ? 9 : 0; }
            };
            if (!pipeline.submit(ChunkIngestor.ingest(4, 5, 1, source))) throw new AssertionError("submit rejected");
            long deadline = System.nanoTime() + 3_000_000_000L;
            while (uploads.queuedCount()==0 && System.nanoTime()<deadline) Thread.sleep(5);
            if (uploads.queuedCount()!=1) throw new AssertionError("worker did not hand mesh to upload queue");
            if (pipeline.acceptedCount()!=1) throw new AssertionError("accepted counter mismatch");
            System.out.println("VoxelMeshPipelineSmokeTest: PASS");
        }
    }
}
