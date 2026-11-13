package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.gpu.gles.GlesRuntimeInfo;
import dev.brezze.voxymobile.gpu.gles.GlesStartupCheck;
import dev.brezze.voxymobile.gpu.gles.OpenGles32GpuDevice;
import dev.brezze.voxymobile.gpu.gles.VoxelTerrainRenderer;
import dev.brezze.voxymobile.gpu.gles.lwjgl.LwjglGles32Api;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Concrete first-party runtime backend using the LWJGL classes already loaded by Minecraft/Zalith. */
public final class LwjglVoxyRenderBackend implements VoxyRenderBackend {
    private final OpenGles32GpuDevice gpu;
    private final VoxelTerrainRenderer terrain;
    private final LodHeightfieldRenderer lodRenderer;
    private long lastStatsLogNanos;
    private int lastLoggedTiles = -1;
    private boolean closed;

    private LwjglVoxyRenderBackend(OpenGles32GpuDevice gpu, VoxelTerrainRenderer terrain) {
        this.gpu = gpu;
        this.terrain = terrain;
        this.lodRenderer = new LodHeightfieldRenderer(gpu, terrain);
    }

    /** Must be called from WorldRenderEvents on the render thread with Minecraft's GL context current. */
    public static LwjglVoxyRenderBackend create() {
        LwjglGles32Api gl = new LwjglGles32Api();
        GlesRuntimeInfo info = gl.queryRuntimeInfo();
        GlesStartupCheck.requireCompatible(info);
        System.out.println("[VoxyMobile] " + GlesStartupCheck.describe(info));

        OpenGles32GpuDevice gpu = new OpenGles32GpuDevice(gl);
        VoxelTerrainRenderer terrain = null;
        try {
            terrain = new VoxelTerrainRenderer(gl, gpu,
                    readResource("/assets/voxymobile/shaders/voxel_terrain.vsh"),
                    readResource("/assets/voxymobile/shaders/voxel_terrain.fsh"));
            System.out.println("[VoxyMobile] Heightfield LOD renderer initialized (bounded 256-chunk history)");
            return new LwjglVoxyRenderBackend(gpu, terrain);
        } catch (RuntimeException | Error failure) {
            if (terrain != null) terrain.close();
            gpu.deleteAllMeshes();
            throw failure;
        }
    }

    @Override public void render(WorldRenderContext context) {
        // The camera-aware callback below is used by VoxyRenderBridge.
    }

    @Override public void renderFrame(WorldRenderContext context,
                                      dev.brezze.voxymobile.render.CameraFrame camera) {
        if (closed) return;
        int tiles = VoxyMobileConfig.lodEnabled
                ? lodRenderer.render(VoxyChunkBridge.getLodSnapshots(), camera.viewProjection(), camera.x(), camera.y(), camera.z())
                : 0;
        long now = System.nanoTime();
        if (tiles != lastLoggedTiles || now - lastStatsLogNanos > 5_000_000_000L) {
            System.out.println("[VoxyMobile] Heightfield LOD tiles drawn: " + tiles
                    + "; retained chunk snapshots: " + VoxyChunkBridge.getLodSnapshots().size());
            lastLoggedTiles = tiles;
            lastStatsLogNanos = now;
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        lodRenderer.close();
        terrain.close();
        gpu.deleteAllMeshes();
    }

    private static String readResource(String path) {
        try (InputStream in = LwjglVoxyRenderBackend.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing VoxyMobile shader resource: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read VoxyMobile shader resource: " + path, failure);
        }
    }
}
