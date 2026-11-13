package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.render.CameraFrame;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Builds a camera-relative LOD matrix independent of Minecraft/Iris far-plane clipping. */
public final class MinecraftCameraFrame {
    private static final float NEAR_PLANE = 0.05f;
    private static final float LOD_FAR_PLANE = 2048.0f;
    private static final float DEFAULT_FOV_DEGREES = 70.0f;

    private MinecraftCameraFrame() {}

    /** Returns null during a transient invalid camera frame instead of detaching the GLES backend. */
    public static CameraFrame capture(WorldRenderContext context) {
        if (context == null) throw new NullPointerException("context");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null || context.camera() == null) return null;
        double cameraX = context.camera().getPos().x;
        double cameraY = context.camera().getPos().y;
        double cameraZ = context.camera().getPos().z;
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) return null;

        Quaternionf cameraRotation = new Quaternionf(context.camera().getRotation());
        if (!Float.isFinite(cameraRotation.x) || !Float.isFinite(cameraRotation.y)
                || !Float.isFinite(cameraRotation.z) || !Float.isFinite(cameraRotation.w)
                || cameraRotation.lengthSquared() < 1.0e-8f) return null;
        cameraRotation.normalize();

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return null;
        float aspect = (float) width / (float) height;
        if (!Float.isFinite(aspect) || aspect <= 0.0f) return null;

        // The model is already camera-relative in the draw backend, so only inverse camera
        // rotation belongs in the view matrix. The longer far plane keeps distant LOD in clip space.
        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(DEFAULT_FOV_DEGREES), aspect, NEAR_PLANE, LOD_FAR_PLANE);
        Matrix4f viewRotation = new Matrix4f().rotation(cameraRotation.conjugate());
        float[] matrix = new float[16];
        projection.mul(viewRotation).get(matrix);
        for (float value : matrix) if (!Float.isFinite(value)) return null;
        return new CameraFrame(matrix, cameraX, cameraY, cameraZ);
    }
}
