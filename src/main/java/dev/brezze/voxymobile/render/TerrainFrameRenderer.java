package dev.brezze.voxymobile.render;

import dev.brezze.voxymobile.gpu.GpuMesh;
import dev.brezze.voxymobile.gpu.gles.VoxelTerrainRenderer;
import dev.brezze.voxymobile.mesh.VoxyMobileRuntime;
import dev.brezze.voxymobile.scheduler.ChunkPos;

/**
 * Connects the runtime's bounded upload phase to camera-relative terrain draw calls.
 * Invoke once per world frame on the same render thread that owns the GLES context.
 */
public final class TerrainFrameRenderer {
    private final VoxyMobileRuntime runtime;
    private final VoxelTerrainRenderer renderer;
    private Thread renderThread;

    public TerrainFrameRenderer(VoxyMobileRuntime runtime, VoxelTerrainRenderer renderer) {
        if (runtime == null || renderer == null) throw new NullPointerException();
        this.runtime = runtime; this.renderer = renderer;
    }

    public TerrainFrameStats render(ChunkRenderList chunks, float[] viewProjection,
                                    double cameraX, double cameraY, double cameraZ,
                                    double previousFrameMillis) {
        checkThread();
        if (chunks == null) throw new NullPointerException("chunks");
        if (viewProjection == null || viewProjection.length != 16)
            throw new IllegalArgumentException("viewProjection must contain 16 floats");
        if (!Double.isFinite(previousFrameMillis) || previousFrameMillis < 0)
            throw new IllegalArgumentException("previousFrameMillis must be finite and non-negative");

        int uploads = runtime.beginFrame(previousFrameMillis);
        int visible = 0, drawn = 0;
        int cameraChunkX = floorChunk(cameraX), cameraChunkZ = floorChunk(cameraZ);
        runtime.updatePlayerChunk(cameraChunkX, cameraChunkZ);
        for (ChunkPos chunk : chunks.chunks()) {
            GpuMesh mesh = runtime.visibleMesh(chunk.x(), chunk.z(), cameraX, cameraZ);
            if (mesh == null) continue;
            visible++;
            renderer.draw(mesh, viewProjection, cameraX, cameraY, cameraZ);
            drawn++;
        }
        return new TerrainFrameStats(uploads, chunks.size(), visible, drawn);
    }

    private static int floorChunk(double blockCoordinate) {
        if (!Double.isFinite(blockCoordinate)) throw new IllegalArgumentException("camera coordinate must be finite");
        double value = Math.floor(blockCoordinate / 16.0);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IllegalArgumentException("camera outside chunk coordinate range");
        return (int) value;
    }

    private void checkThread() {
        Thread now = Thread.currentThread();
        if (renderThread == null) renderThread = now;
        else if (renderThread != now) throw new IllegalStateException("Terrain frame renderer used outside render thread");
    }
}
