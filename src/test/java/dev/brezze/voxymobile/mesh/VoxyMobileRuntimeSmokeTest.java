package dev.brezze.voxymobile.mesh;

import dev.brezze.voxymobile.android.AndroidPerformanceProfile;
import dev.brezze.voxymobile.gpu.GpuDevice;
import dev.brezze.voxymobile.gpu.GpuMesh;
import dev.brezze.voxymobile.ingest.ChunkBlockSource;
import dev.brezze.voxymobile.ingest.ChunkIngestor;
import dev.brezze.voxymobile.ingest.VoxelChunkSnapshot;
import java.util.concurrent.atomic.AtomicInteger;

public final class VoxyMobileRuntimeSmokeTest {
    public static void main(String[] args) throws Exception {
        AtomicInteger created = new AtomicInteger(), deleted = new AtomicInteger();
        GpuDevice gpu = new GpuDevice() {
            public int createMesh(float[] v, int[] i) { created.incrementAndGet(); return created.get(); }
            public void deleteMesh(int h) { deleted.incrementAndGet(); }
        };
        AndroidPerformanceProfile profile = AndroidPerformanceProfile.conservative(8, 256L << 20);
        VoxyMobileRuntime runtime = new VoxyMobileRuntime(gpu, profile, 12, 16, 8L << 20);
        VoxelChunkSnapshot snapshot = ChunkIngestor.ingest(0, 0, 1, new ChunkBlockSource() {
            public int minY() { return 0; }
            public int height() { return 4; }
            public int materialId(int x, int y, int z) { return y == 0 ? 1 : 0; }
        });
        if (!runtime.submit(snapshot)) throw new AssertionError("snapshot rejected");
        long until = System.currentTimeMillis() + 3000;
        while (created.get() == 0 && System.currentTimeMillis() < until) {
            runtime.beginFrame(16.0);
            Thread.sleep(10);
        }
        if (created.get() != 1) throw new AssertionError("mesh did not reach GPU adapter");
        GpuMesh visible = runtime.visibleMesh(0, 0, 8, 8);
        if (visible == null) throw new AssertionError("resident mesh missing");
        if (runtime.visibleMesh(100, 100, 8, 8) != null) throw new AssertionError("distance culler failed");
        runtime.unload(0, 0, 2);
        runtime.beginFrame(16.0);
        if (deleted.get() != 1) throw new AssertionError("unload did not delete resident GPU mesh");
        runtime.close();
        System.out.println("VoxyMobileRuntimeSmokeTest: PASS");
    }
}
