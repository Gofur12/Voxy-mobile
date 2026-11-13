package dev.brezze.voxymobile.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SchedulerSmokeTest {
    public static void main(String[] args) throws Exception {
        boundedQueue(); staleRevisionDiscarded(); successfulCompletion();
        System.out.println("PASS: 3 scheduler smoke tests");
    }
    private static void boundedQueue() {
        try (MeshScheduler<Integer> scheduler = new MeshScheduler<>(1, 1)) {
            CountDownLatch blocker = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            scheduler.submit(new ChunkPos(0, 0), 1, () -> { started.countDown(); blocker.await(); return 1; }, r -> {});
            await(started);
            check(scheduler.submit(new ChunkPos(2, 0), 1, () -> 2, r -> {}), "first queued job accepted");
            check(!scheduler.submit(new ChunkPos(20, 0), 1, () -> 3, r -> {}), "far job rejected at capacity");
            blocker.countDown();
        }
    }
    private static void staleRevisionDiscarded() throws Exception {
        try (MeshScheduler<Integer> scheduler = new MeshScheduler<>(1, 4)) {
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
            AtomicInteger callbacks = new AtomicInteger();
            ChunkPos p = new ChunkPos(4, 5);
            scheduler.submit(p, 1, () -> { started.countDown(); release.await(); return 10; }, r -> callbacks.incrementAndGet());
            await(started);
            scheduler.invalidate(p, 2);
            release.countDown();
            Thread.sleep(80);
            check(callbacks.get() == 0, "stale result discarded");
        }
    }
    private static void successfulCompletion() throws Exception {
        try (MeshScheduler<Integer> scheduler = new MeshScheduler<>(1, 4)) {
            CountDownLatch done = new CountDownLatch(1);
            scheduler.submit(new ChunkPos(0, 0), 7, () -> 42, result -> {
                if (result.succeeded() && result.mesh() == 42 && result.revision() == 7) done.countDown();
            });
            check(done.await(2, TimeUnit.SECONDS), "current result delivered");
        }
    }
    private static void await(CountDownLatch latch) {
        try { check(latch.await(2, TimeUnit.SECONDS), "worker started"); }
        catch (InterruptedException e) { throw new AssertionError(e); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
