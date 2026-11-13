package dev.brezze.voxymobile.android;

import java.util.EnumMap;
import java.util.Map;

/** Thread-safe byte accounting. Reservations must be closed when the owned data is discarded. */
public final class MemoryBudget {
    public enum Pool { SNAPSHOTS, CPU_MESHES, GPU_MESHES }
    private final long totalLimit;
    private final EnumMap<Pool, Long> usage = new EnumMap<>(Pool.class);
    public MemoryBudget(long totalLimit) {
        if (totalLimit < 1) throw new IllegalArgumentException("totalLimit must be positive");
        this.totalLimit=totalLimit; for (Pool p:Pool.values()) usage.put(p,0L);
    }
    public synchronized Reservation tryReserve(Pool pool, long bytes) {
        if (pool==null || bytes<0) throw new IllegalArgumentException("invalid reservation");
        if (bytes > totalLimit - used()) return null;
        usage.put(pool, usage.get(pool)+bytes);
        return new Reservation(this,pool,bytes);
    }
    public synchronized long used() { long n=0; for(long v:usage.values()) n+=v; return n; }
    public synchronized long used(Pool pool) { return usage.get(pool); }
    public long limit() { return totalLimit; }
    private synchronized void release(Pool pool,long bytes) {
        long current=usage.get(pool); if(bytes>current) throw new IllegalStateException("reservation accounting underflow");
        usage.put(pool,current-bytes);
    }
    public static final class Reservation implements AutoCloseable {
        private final MemoryBudget owner; private final Pool pool; private final long bytes; private boolean closed;
        private Reservation(MemoryBudget o,Pool p,long b){owner=o;pool=p;bytes=b;}
        public long bytes(){return bytes;}
        @Override public synchronized void close(){if(!closed){closed=true;owner.release(pool,bytes);}}
    }
}
