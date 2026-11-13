package dev.brezze.voxymobile.gpu.gles.lwjgl;

/** Verifies that the optional adapter has no compile-time LWJGL dependency. */
public final class LwjglGles32ApiSmokeTest {
    public static void main(String[] args) {
        try {
            new LwjglGles32Api();
            System.out.println("LwjglGles32ApiSmokeTest: LWJGL classes found; live GL calls intentionally not issued");
        } catch (IllegalStateException unavailable) {
            if (!unavailable.getMessage().contains("LWJGL OpenGL classes are unavailable")) throw unavailable;
            System.out.println("LwjglGles32ApiSmokeTest: PASS (cleanly reports LWJGL unavailable outside Minecraft)");
        }
    }
}
