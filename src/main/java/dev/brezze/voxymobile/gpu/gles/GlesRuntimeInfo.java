package dev.brezze.voxymobile.gpu.gles;

/** Values queried by the platform-specific GLES binding while a context is current. */
public final class GlesRuntimeInfo {
    public final String version;
    public final String renderer;
    public final int majorVersion;
    public final int minorVersion;
    public final int maxVertexAttributes;

    public GlesRuntimeInfo(String version, String renderer, int majorVersion,
                           int minorVersion, int maxVertexAttributes) {
        this.version = version == null ? "unknown" : version;
        this.renderer = renderer == null ? "unknown" : renderer;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.maxVertexAttributes = maxVertexAttributes;
    }

    public boolean supportsGles32() {
        return majorVersion > 3 || (majorVersion == 3 && minorVersion >= 2);
    }
}
