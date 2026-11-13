package dev.brezze.voxymobile.ingest;

/** One block mutation expressed in chunk-local X/Z and world Y coordinates. */
public final class BlockChange {
    public final int localX, worldY, localZ, materialId;
    public BlockChange(int localX,int worldY,int localZ,int materialId) {
        if(localX<0||localX>15||localZ<0||localZ>15) throw new IllegalArgumentException("local X/Z must be 0..15");
        VoxelChunkSnapshot.validateId(materialId);
        this.localX=localX; this.worldY=worldY; this.localZ=localZ; this.materialId=materialId;
    }
    int key(int minY){return ((worldY-minY)*16+localZ)*16+localX;}
}
