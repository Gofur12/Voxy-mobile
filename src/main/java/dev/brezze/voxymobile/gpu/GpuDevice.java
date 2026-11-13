package dev.brezze.voxymobile.gpu;

/** Render-thread-only adapter. Implement with GLES 3.2 calls inside the Fabric client module. */
public interface GpuDevice {
    int createMesh(float[] vertices, int[] indices);
    void deleteMesh(int handle);
}
