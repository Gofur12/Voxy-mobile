package dev.brezze.voxymobile.fabric;

import dev.brezze.voxymobile.render.CameraFrame;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Copies the 1.20.1 Fabric render matrices and camera position; retains no Minecraft objects. */
public final class MinecraftCameraFrame {
    private MinecraftCameraFrame() {}

    public static CameraFrame capture(WorldRenderContext context) {
        if (context == null) throw new NullPointerException("context");
        MatrixStack stack = context.matrixStack();
        if (stack == null) throw new IllegalStateException("Minecraft matrix stack is unavailable at this render event");
        Vec3d pos = context.camera().getPos();
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z))
            return null;

        // The world-render matrix stack already contains the camera translation in 1.20.1.
        // Our GLES shader also applies a camera-relative chunk offset, so retaining that
        // translation here subtracts the camera twice and pushes every LOD tile off-screen.
        // Keep the view rotation, remove only translation, then apply projection.
        Matrix4f viewRotation = new Matrix4f(stack.peek().getPositionMatrix());
        viewRotation.m30(0.0f).m31(0.0f).m32(0.0f);
        Matrix4f combined = new Matrix4f(context.projectionMatrix()).mul(viewRotation);
        float[] matrix = new float[16];
        combined.get(matrix);
        for (float value : matrix) if (!Float.isFinite(value)) return null;
        return new CameraFrame(matrix, pos.x, pos.y, pos.z);
    }
}
