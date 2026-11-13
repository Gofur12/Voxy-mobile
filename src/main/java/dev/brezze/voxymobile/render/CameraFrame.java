package dev.brezze.voxymobile.render;

/** Immutable camera data copied out of Minecraft's render context for one frame. */
public final class CameraFrame {
    private final float[] viewProjection;
    private final double x, y, z;

    public CameraFrame(float[] viewProjection, double x, double y, double z) {
        if (viewProjection == null || viewProjection.length != 16)
            throw new IllegalArgumentException("viewProjection must contain 16 floats");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("camera coordinates must be finite");
        this.viewProjection = viewProjection.clone();
        for (float value : this.viewProjection) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("matrix values must be finite");
        }
        this.x = x; this.y = y; this.z = z;
    }

    public float[] viewProjection() { return viewProjection.clone(); }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    /** Multiplies two column-major 4x4 matrices: result = left * right. */
    public static float[] multiply(float[] left, float[] right) {
        if (left == null || right == null || left.length != 16 || right.length != 16)
            throw new IllegalArgumentException("both matrices must contain 16 floats");
        float[] out = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++) sum += left[k * 4 + row] * right[col * 4 + k];
                out[col * 4 + row] = sum;
            }
        }
        return out;
    }
}
