package dev.brezze.voxymobile.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded per-chunk change coalescer: repeated edits to one block keep only the newest ID. */
public final class BlockChangeAccumulator {
    private final int minY, height, capacity;
    private final Map<Integer,BlockChange> pending=new LinkedHashMap<>();
    public BlockChangeAccumulator(int minY,int height,int capacity) {
        if(height<1||height>4096||capacity<1) throw new IllegalArgumentException("Invalid height/capacity");
        this.minY=minY; this.height=height; this.capacity=capacity;
    }
    public synchronized boolean offer(BlockChange change) {
        if(change.worldY<minY||change.worldY>=minY+height) return false;
        int key=change.key(minY);
        if(!pending.containsKey(key)&&pending.size()>=capacity) return false;
        pending.put(key,change); return true;
    }
    public synchronized int pendingCount(){return pending.size();}
    public synchronized VoxelChunkSnapshot applyTo(VoxelChunkSnapshot snapshot,long newRevision) {
        if(snapshot.minY()!=minY||snapshot.height()!=height) throw new IllegalArgumentException("Accumulator range differs from snapshot");
        VoxelChunkSnapshot updated=snapshot.withChanges(pending.values(),newRevision);
        pending.clear(); return updated;
    }
    public synchronized void clear(){pending.clear();}
}
