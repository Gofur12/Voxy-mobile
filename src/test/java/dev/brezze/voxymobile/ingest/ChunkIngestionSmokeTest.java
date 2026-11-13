package dev.brezze.voxymobile.ingest;

public final class ChunkIngestionSmokeTest {
    public static void main(String[] args) {
        VoxelChunkSnapshot snapshot=ChunkIngestor.ingest(3,-2,10,new ChunkBlockSource(){
            public int minY(){return -64;} public int height(){return 384;}
            public int materialId(int x,int y,int z){return y<0?0:(y==0?1:2+((x+z*16+y*256)%300));}
        });
        check(snapshot.chunkX()==3&&snapshot.chunkZ()==-2,"chunk coordinates");
        check(snapshot.minY()==-64&&snapshot.maxYExclusive()==320,"dynamic vertical range");
        check(snapshot.materialId(0,-64,0)==0&&snapshot.materialId(0,0,0)==1,"block sampling");
        check(!snapshot.uses8BitIndices(),"palette over 256 must use 16-bit indices");
        BlockChangeAccumulator changes=new BlockChangeAccumulator(-64,384,2);
        check(changes.offer(new BlockChange(1,12,2,400)),"first delta");
        check(changes.offer(new BlockChange(1,12,2,401)),"coalesce same block");
        check(changes.offer(new BlockChange(2,12,2,5)),"second delta");
        check(!changes.offer(new BlockChange(3,12,2,6)),"bounded queue");
        VoxelChunkSnapshot updated=changes.applyTo(snapshot,11);
        check(updated.revision()==11&&updated.materialId(1,12,2)==401&&updated.materialId(2,12,2)==5,"delta apply");
        check(changes.pendingCount()==0,"drain clears pending changes");
        VoxelChunkSnapshot compact=ChunkIngestor.ingest(0,0,1,new ChunkBlockSource(){
            public int minY(){return 0;} public int height(){return(1);}
            public int materialId(int x,int y,int z){return (x+z)%2;}
        });
        check(compact.uses8BitIndices()&&compact.paletteSize()==2,"8-bit palette path");
        System.out.println("ChunkIngestionSmokeTest: PASS (384-height, palettes, bounded/coalesced deltas)");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
