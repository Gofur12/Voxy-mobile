package dev.brezze.voxymobile.gpu.gles;

public final class GlesStartupCheckSmokeTest {
    public static void main(String[] args) {
        GlesRuntimeInfo adreno = new GlesRuntimeInfo("OpenGL ES 3.2 V@...", "Adreno (TM) 640", 3, 2, 16);
        GlesStartupCheck.requireCompatible(adreno);
        if (!GlesStartupCheck.describe(adreno).contains("Adreno (TM) 640")) throw new AssertionError("diagnostic missing renderer");
        expectFailure(new GlesRuntimeInfo("OpenGL ES 3.1", "test", 3, 1, 16));
        expectFailure(new GlesRuntimeInfo("OpenGL ES 3.2", "test", 3, 2, 1));
        boolean missingRejected = false;
        try { GlesStartupCheck.requireCompatible(null); }
        catch (IllegalStateException expected) { missingRejected = true; }
        if (!missingRejected) throw new AssertionError("missing runtime info accepted");
        System.out.println("GlesStartupCheckSmokeTest: PASS");
    }
    private static void expectFailure(GlesRuntimeInfo info) {
        try { GlesStartupCheck.requireCompatible(info); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("incompatible GLES device accepted");
    }
}
