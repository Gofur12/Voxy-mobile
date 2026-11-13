package dev.brezze.voxymobile.scheduler;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Bounded background meshing scheduler.
 * Newer revisions replace queued work; completed stale work is discarded.
 * Completion callbacks execute on worker threads and should enqueue GPU uploads
 * onto the render thread rather than calling OpenGL directly.
 */
public final class MeshScheduler<T> implements AutoCloseable {
    private final Object lock = new Object();
    private final PriorityQueue<Job<T>> queue = new PriorityQueue<>();
    private final Map<Long, Job<T>> queuedByChunk = new HashMap<>();
    private final Map<Long, Long> latestRevision = new HashMap<>();
    private final List<Thread> workers = new ArrayList<>();
    private final int maxQueued;
    private final AtomicBoolean closed = new AtomicBoolean();
    private long sequence;
    private int centerX, centerZ;

    public MeshScheduler(int workerCount, int maxQueued) {
        if (workerCount < 1 || maxQueued < 1) throw new IllegalArgumentException("workerCount and maxQueued must be positive");
        this.maxQueued = maxQueued;
        for (int i = 0; i < workerCount; i++) {
            Thread thread = new Thread(this::workerLoop, "VoxyMobile-Mesh-" + i);
            thread.setDaemon(true);
            workers.add(thread);
            thread.start();
        }
    }

    /** Update player chunk; queued priorities are recalculated. */
    public void updateCenter(int chunkX, int chunkZ) {
        synchronized (lock) {
            centerX = chunkX;
            centerZ = chunkZ;
            if (queue.size() > 1) {
                List<Job<T>> jobs = new ArrayList<>(queue);
                queue.clear();
                for (Job<T> job : jobs) { job.distance = job.position.distanceSquared(centerX, centerZ); queue.add(job); }
            }
            lock.notifyAll();
        }
    }

    /** Submit or replace work for a chunk. Returns false if rejected by queue limits. */
    public boolean submit(ChunkPos position, long revision, Callable<T> mesher, Consumer<MeshResult<T>> completion) {
        Objects.requireNonNull(position); Objects.requireNonNull(mesher); Objects.requireNonNull(completion);
        synchronized (lock) {
            if (closed.get()) return false;
            long key = position.packed();
            Long known = latestRevision.get(key);
            if (known != null && revision < known) return false;
            latestRevision.put(key, revision);
            Job<T> old = queuedByChunk.remove(key);
            if (old != null) queue.remove(old);
            Job<T> job = new Job<>(position, revision, sequence++, position.distanceSquared(centerX, centerZ), mesher, completion);
            if (queue.size() >= maxQueued) {
                Job<T> farthest = Collections.max(queue);
                if (job.compareTo(farthest) >= 0) return false;
                queue.remove(farthest);
                queuedByChunk.remove(farthest.position.packed(), farthest);
            }
            queue.add(job);
            queuedByChunk.put(key, job);
            lock.notifyAll();
            return true;
        }
    }

    /** Mark a chunk revision as changed; older queued/running results become invalid. */
    public void invalidate(ChunkPos position, long newRevision) {
        synchronized (lock) {
            long key = position.packed();
            latestRevision.merge(key, newRevision, Math::max);
            Job<T> queued = queuedByChunk.remove(key);
            if (queued != null) queue.remove(queued);
        }
    }

    public int queuedCount() { synchronized (lock) { return queue.size(); } }

    private void workerLoop() {
        while (!closed.get()) {
            Job<T> job;
            synchronized (lock) {
                while (queue.isEmpty() && !closed.get()) {
                    try { lock.wait(); } catch (InterruptedException e) { if (closed.get()) return; }
                }
                if (closed.get()) return;
                job = queue.poll();
                queuedByChunk.remove(job.position.packed(), job);
            }
            T mesh = null; Throwable error = null;
            try { mesh = job.mesher.call(); } catch (Throwable t) { error = t; }
            boolean current;
            synchronized (lock) {
                current = Objects.equals(latestRevision.get(job.position.packed()), job.revision);
            }
            if (current && !closed.get()) {
                try { job.completion.accept(new MeshResult<>(job.position, job.revision, mesh, error)); }
                catch (RuntimeException ignored) { /* A consumer must not kill a worker. */ }
            }
        }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (lock) { queue.clear(); queuedByChunk.clear(); lock.notifyAll(); }
        for (Thread worker : workers) worker.interrupt();
    }

    private static final class Job<T> implements Comparable<Job<T>> {
        final ChunkPos position; final long revision, sequence; final Callable<T> mesher;
        final Consumer<MeshResult<T>> completion; long distance;
        Job(ChunkPos p, long r, long s, long d, Callable<T> m, Consumer<MeshResult<T>> c) {
            position=p; revision=r; sequence=s; distance=d; mesher=m; completion=c;
        }
        @Override public int compareTo(Job<T> other) {
            int byDistance = Long.compare(distance, other.distance);
            return byDistance != 0 ? byDistance : Long.compare(sequence, other.sequence);
        }
    }
}
