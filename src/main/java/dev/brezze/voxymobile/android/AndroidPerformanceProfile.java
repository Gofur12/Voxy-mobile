package dev.brezze.voxymobile.android;

/** Conservative device profile for Android GLES clients; all budgets are caller-overridable. */
public final class AndroidPerformanceProfile {
    public final int workerCount;
    public final int maxQueuedJobs;
    public final int maxUploadsPerFrame;
    public final long uploadBytesPerFrame;
    public final long cpuCacheBudgetBytes;
    public final long gpuBudgetBytes;

    private AndroidPerformanceProfile(int workers, int queued, int uploads, long uploadBytes,
                                      long cpuBytes, long gpuBytes) {
        workerCount=workers; maxQueuedJobs=queued; maxUploadsPerFrame=uploads;
        uploadBytesPerFrame=uploadBytes; cpuCacheBudgetBytes=cpuBytes; gpuBudgetBytes=gpuBytes;
    }

    /** heapMaxBytes is Runtime.maxMemory(), not total physical RAM. */
    public static AndroidPerformanceProfile conservative(int availableProcessors, long heapMaxBytes) {
        if (availableProcessors < 1 || heapMaxBytes < 64L * 1024 * 1024)
            throw new IllegalArgumentException("invalid device limits");
        int workers = Math.max(1, Math.min(2, availableProcessors - 1));
        long cpu = clamp(heapMaxBytes / 8, 24L << 20, 96L << 20);
        long gpu = clamp(heapMaxBytes / 6, 24L << 20, 128L << 20);
        long upload = Math.min(2L << 20, Math.max(256L << 10, gpu / 32));
        return new AndroidPerformanceProfile(workers, 24, 2, upload, cpu, gpu);
    }
    private static long clamp(long n, long min, long max) { return Math.max(min, Math.min(max, n)); }
}
