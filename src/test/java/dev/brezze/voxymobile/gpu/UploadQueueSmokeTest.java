package dev.brezze.voxymobile.gpu;

public final class UploadQueueSmokeTest {
    private static final class FakeGpu implements GpuDevice {
        int next = 1, creates, deletes;
        public int createMesh(float[] v, int[] i) { creates++; return next++; }
        public void deleteMesh(int h) { deletes++; }
    }
    private static MeshPayload mesh(int x, long rev, int floats) {
        return new MeshPayload(x, 0, rev, new float[floats], new int[]{0,1,2});
    }
    public static void main(String[] args) {
        FakeGpu gpu = new FakeGpu();
        MeshUploadQueue q = new MeshUploadQueue(4, 4096);
        if (!q.offer(mesh(0, 1, 12))) throw new AssertionError("offer");
        if (!q.offer(mesh(0, 2, 12))) throw new AssertionError("replace queued revision");
        if (q.queuedCount() != 1 || q.queuedBytes() != 60) throw new AssertionError("bounded replacement");
        if (q.offer(mesh(0, 1, 12))) throw new AssertionError("stale revision accepted");
        if (q.drain(gpu, 1, 1024) != 1) throw new AssertionError("drain");
        if (q.get(0,0) == null || q.get(0,0).revision() != 2) throw new AssertionError("wrong resident revision");
        if (!q.offer(mesh(0, 3, 12))) throw new AssertionError("new revision");
        q.drain(gpu, 1, 1024);
        if (gpu.creates != 2 || gpu.deletes != 1 || q.get(0,0).revision() != 3) throw new AssertionError("replace resident");
        q.remove(0,0,gpu);
        if (q.get(0,0) != null || gpu.deletes != 2) throw new AssertionError("remove resident");
        if (q.offer(mesh(1, 1, 2000))) throw new AssertionError("oversized payload accepted");
        if (!q.offer(mesh(2, 1, 12))) throw new AssertionError("offer before clear");
        q.drain(gpu, 1, 1024);
        if (q.get(2,0) == null) throw new AssertionError("resident before clear missing");
        q.clear(gpu);
        if (q.get(2,0) != null || q.queuedCount() != 0 || gpu.deletes != 3)
            throw new AssertionError("clear did not release all resident GPU resources");
        System.out.println("UploadQueueSmokeTest: PASS (replacement, stale rejection, drain, eviction, byte cap, full clear)");
    }
}
