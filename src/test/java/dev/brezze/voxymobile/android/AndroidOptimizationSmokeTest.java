package dev.brezze.voxymobile.android;

public final class AndroidOptimizationSmokeTest {
    public static void main(String[] args) {
        AndroidPerformanceProfile p=AndroidPerformanceProfile.conservative(8, 1024L<<20);
        check(p.workerCount==2 && p.maxQueuedJobs>0 && p.gpuBudgetBytes>0,"profile");
        MemoryBudget b=new MemoryBudget(1000);
        MemoryBudget.Reservation a=b.tryReserve(MemoryBudget.Pool.SNAPSHOTS,700);
        check(a!=null && b.used()==700,"reserve");
        check(b.tryReserve(MemoryBudget.Pool.GPU_MESHES,301)==null,"hard cap");
        a.close(); check(b.used()==0,"release"); a.close();
        AdaptiveUploadBudget adaptive=new AdaptiveUploadBudget(1,4,128<<10,2L<<20);
        int before=adaptive.maxUploads(); adaptive.observeFrameMillis(30); check(adaptive.maxUploads()<=before,"slow frame reduction");
        for(int i=0;i<12;i++) adaptive.observeFrameMillis(10);
        check(adaptive.maxUploads()==4 && adaptive.maxBytes()==2L<<20,"fast frame recovery cap");
        ChunkDistanceCuller c=new ChunkDistanceCuller(8);
        check(c.isVisible(0,0,8,8),"near chunk"); check(!c.isVisible(30,30,8,8),"far chunk");
        System.out.println("AndroidOptimizationSmokeTest: PASS");
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
