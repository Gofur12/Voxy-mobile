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
        Matrix4f combined = new Matrix4f(context.projectionMatrix()).mul(stack.peek().getPositionMatrix());
        float[] matrix = new float[16];
        combined.get(matrix);
        return new CameraFrame(matrix, pos.x, pos.y, pos.z);
    }
}
