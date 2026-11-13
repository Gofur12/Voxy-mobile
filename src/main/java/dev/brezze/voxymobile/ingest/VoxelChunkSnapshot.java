package dev.brezze.voxymobile.ingest;

import java.util.HashMap;
import java.util.Map;

/** Immutable palette-compressed 16xheightx16 chunk snapshot. Index order is (y*16+z)*16+x. */
public final class VoxelChunkSnapshot {
    private final int chunkX, chunkZ, minY, height;
    private final long revision;
    private final int[] palette;
    private final byte[] indices8;
    private final char[] indices16;

    private VoxelChunkSnapshot(int chunkX, int chunkZ, int minY, int height, long revision,
                               int[] palette, byte[] indices8, char[] indices16) {
        this.chunkX=chunkX; this.chunkZ=chunkZ; this.minY=minY; this.height=height; this.revision=revision;
        this.palette=palette; this.indices8=indices8; this.indices16=indices16;
    }

    static VoxelChunkSnapshot compress(int chunkX, int chunkZ, int minY, int height, long revision, int[] ids) {
        Map<Integer,Integer> map = new HashMap<>();
        int[] palette = new int[Math.min(ids.length, 256)];
        int paletteSize=0;
        int[] temp = new int[ids.length];
        for (int i=0;i<ids.length;i++) {
            int id=ids[i]; validateId(id);
            Integer index=map.get(id);
            if (index==null) {
                if (paletteSize==65536) throw new IllegalArgumentException("More than 65536 material IDs in one chunk");
                if (paletteSize==palette.length) palette=java.util.Arrays.copyOf(palette, Math.min(65536, Math.max( palette.length*2, 1)));
                index=paletteSize; map.put(id,index); palette[paletteSize++]=id;
            }
            temp[i]=index;
        }
        palette=java.util.Arrays.copyOf(palette,paletteSize);
        if (paletteSize<=256) {
            byte[] packed=new byte[ids.length];
            for(int i=0;i<temp.length;i++) packed[i]=(byte)temp[i];
            return new VoxelChunkSnapshot(chunkX,chunkZ,minY,height,revision,palette,packed,null);
        }
        char[] packed=new char[ids.length];
        for(int i=0;i<temp.length;i++) packed[i]=(char)temp[i];
        return new VoxelChunkSnapshot(chunkX,chunkZ,minY,height,revision,palette,null,packed);
    }

    public int chunkX(){return chunkX;} public int chunkZ(){return chunkZ;}
    public int minY(){return minY;} public int height(){return height;} public int maxYExclusive(){return minY+height;}
    public long revision(){return revision;} public int paletteSize(){return palette.length;}
    public boolean uses8BitIndices(){return indices8!=null;}
    public long storageBytes(){return (long)palette.length*4 + (indices8!=null?indices8.length:indices16.length)* (indices8!=null?1:2);}
    public int materialId(int localX,int worldY,int localZ) {
        if(localX<0||localX>15||localZ<0||localZ>15||worldY<minY||worldY>=minY+height)
            throw new IndexOutOfBoundsException("Block outside snapshot");
        int offset=((worldY-minY)*16+localZ)*16+localX;
        int paletteIndex=indices8!=null?(indices8[offset]&255):indices16[offset];
        return palette[paletteIndex];
    }
    public int[] paletteCopy(){return palette.clone();}

    /** Applies a coalesced batch and returns a new immutable snapshot with the supplied revision. */
    public VoxelChunkSnapshot withChanges(Iterable<BlockChange> changes,long newRevision) {
        if(newRevision<revision) throw new IllegalArgumentException("Revision cannot move backwards");
        int[] ids=new int[16*16*height];
        for(int y=0;y<height;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
            ids[(y*16+z)*16+x]=materialId(x,minY+y,z);
        for(BlockChange c:changes) {
            if(c.localX<0||c.localX>15||c.localZ<0||c.localZ>15||c.worldY<minY||c.worldY>=minY+height)
                continue;
            validateId(c.materialId);
            ids[((c.worldY-minY)*16+c.localZ)*16+c.localX]=c.materialId;
        }
        return compress(chunkX,chunkZ,minY,height,newRevision,ids);
    }
    static void validateId(int id){if(id<0||id>65535) throw new IllegalArgumentException("materialId must be 0..65535");}
}
