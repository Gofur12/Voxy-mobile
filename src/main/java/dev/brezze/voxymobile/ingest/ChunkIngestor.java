package dev.brezze.voxymobile.ingest;

/** Copies a loaded Minecraft chunk through a small adapter, without retaining Minecraft objects. */
public final class ChunkIngestor {
    private static final int MAX_HEIGHT=4096;
    private ChunkIngestor() {}
    public static VoxelChunkSnapshot ingest(int chunkX,int chunkZ,long revision,ChunkBlockSource source) {
        if(source==null) throw new NullPointerException("source");
        int minY=source.minY(), height=source.height();
        if(height<1||height>MAX_HEIGHT) throw new IllegalArgumentException("Chunk height must be 1.."+MAX_HEIGHT);
        long max=(long)minY+height;
        if(max>Integer.MAX_VALUE||max<Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid vertical range");
        int[] ids=new int[16*16*height];
        for(int y=0;y<height;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            int id=source.materialId(x,minY+y,z);
            VoxelChunkSnapshot.validateId(id);
            ids[(y*16+z)*16+x]=id;
        }
        return VoxelChunkSnapshot.compress(chunkX,chunkZ,minY,height,revision,ids);
    }
}
