package dev.brezze.voxymobile.gpu;

public final class GpuMesh {
    private final int handle, chunkX, chunkZ;
    private final long revision, uploadedBytes;
    public GpuMesh(int handle, int chunkX, int chunkZ, long revision, long uploadedBytes) {
        this.handle = handle; this.chunkX = chunkX; this.chunkZ = chunkZ;
        this.revision = revision; this.uploadedBytes = uploadedBytes;
    }
    public int handle() { return handle; }
    public int chunkX() { return chunkX; }
    public int chunkZ() { return chunkZ; }
    public long revision() { return revision; }
    public long uploadedBytes() { return uploadedBytes; }
}
