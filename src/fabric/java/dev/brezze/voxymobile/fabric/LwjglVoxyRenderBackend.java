package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.android.AndroidPerformanceProfile;
import dev.brezze.voxymobile.gpu.gles.GlesRuntimeInfo;
import dev.brezze.voxymobile.gpu.gles.GlesStartupCheck;
import dev.brezze.voxymobile.gpu.gles.OpenGles32GpuDevice;
import dev.brezze.voxymobile.gpu.gles.VoxelTerrainRenderer;
import dev.brezze.voxymobile.gpu.gles.lwjgl.LwjglGles32Api;
import dev.brezze.voxymobile.mesh.VoxyMobileRuntime;
import dev.brezze.voxymobile.render.ChunkRenderList;
import dev.brezze.voxymobile.render.TerrainFrameRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Concrete first-party runtime backend using the LWJGL classes already loaded by Minecraft/Zalith. */
public final class LwjglVoxyRenderBackend implements VoxyRenderBackend {
    private final OpenGles32GpuDevice gpu;
    private final VoxelTerrainRenderer terrain;
    private final VoxyMobileRuntime runtime;
    private final TerrainFrameRenderer frames;
    private long lastFrameNanos;
    private boolean closed;

    private LwjglVoxyRenderBackend(LwjglGles32Api gl, OpenGles32GpuDevice gpu,
                                   VoxelTerrainRenderer terrain, VoxyMobileRuntime runtime) {
        this.gpu = gpu;
        this.terrain = terrain;
        this.runtime = runtime;
        this.frames = new TerrainFrameRenderer(runtime, terrain);
        this.lastFrameNanos = System.nanoTime();
        VoxyChunkBridge.attachRuntime(runtime);
    }

    /** Must be called from WorldRenderEvents on the render thread with Minecraft's GL context current. */
    public static LwjglVoxyRenderBackend create() {
        LwjglGles32Api gl = new LwjglGles32Api();
        GlesRuntimeInfo info = gl.queryRuntimeInfo();
        GlesStartupCheck.requireCompatible(info);
        System.out.println("[VoxyMobile] " + GlesStartupCheck.describe(info));

        OpenGles32GpuDevice gpu = new OpenGles32GpuDevice(gl);
        VoxelTerrainRenderer terrain = null;
        VoxyMobileRuntime runtime = null;
        try {
            terrain = new VoxelTerrainRenderer(gl, gpu,
                    readResource("/assets/voxymobile/shaders/voxel_terrain.vsh"),
                    readResource("/assets/voxymobile/shaders/voxel_terrain.fsh"));
            AndroidPerformanceProfile profile = AndroidPerformanceProfile.conservative(
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory());
            runtime = new VoxyMobileRuntime(gpu, profile, 8, 24, 8L << 20);
            return new LwjglVoxyRenderBackend(gl, gpu, terrain, runtime);
        } catch (RuntimeException | Error failure) {
            if (runtime != null) runtime.close();
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
        long now = System.nanoTime();
        double previousMillis = Math.max(0.0, (now - lastFrameNanos) / 1_000_000.0);
        lastFrameNanos = now;
        frames.render(new ChunkRenderList(VoxyChunkBridge.getLoadedChunkPositions()),
                camera.viewProjection(), camera.x(), camera.y(), camera.z(), previousMillis);
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        VoxyChunkBridge.attachRuntime(null);
        runtime.close(); // includes render-thread GPU mesh deletion
        terrain.close();
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
