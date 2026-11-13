package dev.brezze.voxymobile.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

/** Render-thread gate between Fabric's world render event and a launcher GLES backend. */
public final class VoxyRenderBridge {
    private static VoxyRenderBackend backend;
    private static Thread renderThread;
    private static boolean enabled = true;
    private static boolean autoCreateAttempted;
    private static long nextCreateAttemptNanos;
    private static final long CREATE_RETRY_DELAY_NANOS = 2_000_000_000L;

    private VoxyRenderBridge() {}

    /** Register only after the launcher has a current GLES context. */
    public static void attach(VoxyRenderBackend value) {
        if (value == null) throw new NullPointerException("backend");
        checkRenderThread();
        if (backend != null && backend != value) backend.close();
        backend = value;
        autoCreateAttempted = true;
        nextCreateAttemptNanos = 0L;
        enabled = true;
    }

    /** Called by WorldRenderEvents.LAST. No GL calls are made if no backend is installed. */
    public static void render(WorldRenderContext context) {
        if (context == null || !enabled) return;
        checkRenderThread();
        VoxyRenderBackend current = backend;
        long now = System.nanoTime();
        if (current == null && (!autoCreateAttempted || now >= nextCreateAttemptNanos)) {
            autoCreateAttempted = true;
            try {
                current = LwjglVoxyRenderBackend.create();
                backend = current;
                nextCreateAttemptNanos = 0L;
            } catch (RuntimeException | LinkageError failure) {
                nextCreateAttemptNanos = now + CREATE_RETRY_DELAY_NANOS;
                System.err.println("[VoxyMobile] GLES backend initialization failed; retrying in 2s: " + failure);
                return;
            }
        }
        if (current != null) {
            try {
                dev.brezze.voxymobile.render.CameraFrame camera = MinecraftCameraFrame.capture(context);
                // Projection matrices can be transiently invalid during world transitions.
                // Skip that frame instead of destroying a healthy GLES backend.
                if (camera != null) current.renderFrame(context, camera);
            } catch (RuntimeException | LinkageError failure) {
                // A renderer fault must not take down the whole Minecraft client.
                // We are inside the world-render callback, so teardown is still on the GL thread.
                System.err.println("[VoxyMobile] Render frame failed; detaching backend and retrying in 2s: " + failure);
                backend = null;
                nextCreateAttemptNanos = now + CREATE_RETRY_DELAY_NANOS;
                autoCreateAttempted = true;
                try {
                    current.close();
                } catch (RuntimeException | LinkageError closeFailure) {
                    System.err.println("[VoxyMobile] Backend cleanup after render failure also failed: " + closeFailure);
                }
            }
        }
    }

    /** Re-enable rendering for a newly joined world after a disconnect teardown. */
    public static void enable() {
        checkRenderThread();
        enabled = true;
        autoCreateAttempted = false;
        nextCreateAttemptNanos = 0L;
    }

    /** Disable drawing immediately; backend destruction must happen on the render thread. */
    public static void disable() { enabled = false; }

    public static void setEnabled(boolean value) {
        if (value) enable(); else disable();
    }

    /** Render-thread-only explicit teardown, before the GLES context is destroyed. */
    public static void detach() {
        checkRenderThread();
        enabled = false;
        VoxyRenderBackend old = backend;
        backend = null;
        autoCreateAttempted = false;
        nextCreateAttemptNanos = 0L;
        if (old != null) old.close();
    }

    public static boolean isAttached() { return backend != null; }

    private static void checkRenderThread() {
        Thread current = Thread.currentThread();
        if (renderThread == null) renderThread = current;
        else if (renderThread != current) throw new IllegalStateException("VoxyMobile render bridge used outside render thread");
    }
}
