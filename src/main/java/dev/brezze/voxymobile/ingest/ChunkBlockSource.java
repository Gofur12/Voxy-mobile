package dev.brezze.voxymobile.ingest;

/** Adapter boundary: implement this on the Minecraft client thread using a loaded chunk. */
public interface ChunkBlockSource {
    int minY();
    int height();
    /** Return a stable renderer material ID in the unsigned-16-bit range; 0 is air. */
    int materialId(int localX, int worldY, int localZ);
}
