package dev.brezze.voxymobile.gpu.gles;

/** Fail-fast checks for the exact shader and vertex layout used by VoxyMobile. */
public final class GlesStartupCheck {
    private GlesStartupCheck() {}

    public static void requireCompatible(GlesRuntimeInfo info) {
        if (info == null) throw new IllegalStateException("GLES runtime information was not supplied; device compatibility is unverified");
        if (!info.supportsGles32()) {
            throw new IllegalStateException("VoxyMobile requires OpenGL ES 3.2 for its #version 320 es shaders; detected " + info.version);
        }
        if (info.maxVertexAttributes < 2) {
            throw new IllegalStateException("VoxyMobile requires at least 2 vertex attributes; detected " + info.maxVertexAttributes);
        }
    }

    public static String describe(GlesRuntimeInfo info) {
        if (info == null) return "GLES: not queried";
        return "GLES " + info.majorVersion + "." + info.minorVersion
                + " | renderer=" + info.renderer
                + " | maxVertexAttributes=" + info.maxVertexAttributes;
    }
}
