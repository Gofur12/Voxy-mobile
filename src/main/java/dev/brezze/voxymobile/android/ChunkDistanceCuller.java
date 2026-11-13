package dev.brezze.voxymobile.android;

/** Cheap horizontal distance gate before optional frustum testing. */
public final class ChunkDistanceCuller {
    private final long maxDistanceSquared;
    public ChunkDistanceCuller(int viewDistanceChunks) {
        if(viewDistanceChunks<1) throw new IllegalArgumentException("viewDistanceChunks must be positive");
        long blocks=(long)viewDistanceChunks*16L; maxDistanceSquared=blocks*blocks;
    }
    public boolean isVisible(int chunkX,int chunkZ,double cameraBlockX,double cameraBlockZ) {
        double centerX=chunkX*16.0+8.0, centerZ=chunkZ*16.0+8.0;
        double dx=centerX-cameraBlockX,dz=centerZ-cameraBlockZ;
        return dx*dx+dz*dz<=maxDistanceSquared;
    }
}
