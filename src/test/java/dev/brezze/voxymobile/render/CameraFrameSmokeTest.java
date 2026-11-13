package dev.brezze.voxymobile.render;

public final class CameraFrameSmokeTest {
    public static void main(String[] args) {
        float[] identity = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        float[] translated = {1,0,0,0, 0,1,0,0, 0,0,1,0, 4,5,6,1};
        float[] result = CameraFrame.multiply(identity, translated);
        for (int i = 0; i < 16; i++) if (result[i] != translated[i]) throw new AssertionError("identity multiplication");
        CameraFrame frame = new CameraFrame(result, 12.5, 70, -3.25);
        float[] leaked = frame.viewProjection(); leaked[0] = 99;
        if (frame.viewProjection()[0] != 1) throw new AssertionError("matrix must be defensive copied");
        if (frame.x() != 12.5 || frame.y() != 70 || frame.z() != -3.25) throw new AssertionError("camera coordinates");
        boolean rejected = false;
        try { new CameraFrame(identity, Double.NaN, 0, 0); } catch (IllegalArgumentException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("non-finite camera should be rejected");
        System.out.println("CameraFrameSmokeTest: PASS");
    }
}
