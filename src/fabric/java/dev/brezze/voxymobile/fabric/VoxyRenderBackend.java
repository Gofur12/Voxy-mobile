package dev.brezze.voxymobile.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

/**
 * Launcher-specific render backend SPI. Implementations create the GLES binding,
 * VoxyMobileRuntime and VoxelTerrainRenderer while a GLES context is current.
 * All methods are invoked from Fabric's world-render callback (the render thread).
 */
public interface VoxyRenderBackend extends AutoCloseable {
    /** Called once per world render pass. Must not retain WorldRenderContext. */
    void render(WorldRenderContext context);

    /** Preferred frame callback with copied camera/view-projection data. */
    default void renderFrame(WorldRenderContext context, dev.brezze.voxymobile.render.CameraFrame camera) {
        render(context);
    }

    /** Called on the render thread when this backend is detached or the client stops. */
    @Override void close();
}
