package dev.brezze.voxymobile.gpu.gles;

import dev.brezze.voxymobile.gpu.GpuMesh;

/** Small shader + draw coordinator. Call create/draw/delete only on the render thread. */
public final class VoxelTerrainRenderer implements AutoCloseable {
    private final Gles32Api gl;
    private final OpenGles32GpuDevice device;
    private final int program;
    private final int mvpLocation, offsetLocation;
    private Thread renderThread;

    public VoxelTerrainRenderer(Gles32Api gl, OpenGles32GpuDevice device, String vertexSource, String fragmentSource) {
        if (gl == null || device == null || vertexSource == null || fragmentSource == null) throw new NullPointerException();
        this.gl = gl; this.device = device;
        int vs = compile(Gles32Api.GL_VERTEX_SHADER, vertexSource);
        int fs = 0;
        int linked = 0;
        try {
            fs = compile(Gles32Api.GL_FRAGMENT_SHADER, fragmentSource);
            linked = gl.createProgram();
            gl.attachShader(linked, vs);
            gl.attachShader(linked, fs);
            gl.bindAttribLocation(linked, 0, "aPosition");
            gl.bindAttribLocation(linked, 1, "aMaterial");
            gl.linkProgram(linked);
            if (!gl.programLinkStatus(linked)) throw new IllegalStateException("Voxel shader link failed: " + gl.programInfoLog(linked));
            program = linked;
            mvpLocation = gl.getUniformLocation(program, "uMvp");
            offsetLocation = gl.getUniformLocation(program, "uChunkOffset");
            if (mvpLocation < 0 || offsetLocation < 0) throw new IllegalStateException("Required voxel shader uniform missing");
        } catch (RuntimeException | Error failure) {
            if (linked != 0) gl.deleteProgram(linked);
            throw failure;
        } finally {
            gl.deleteShader(vs);
            if (fs != 0) gl.deleteShader(fs);
        }
    }

    /** Draw using a view-projection matrix and camera-relative chunk translation. */
    public void draw(GpuMesh mesh, float[] viewProjectionMatrix, double cameraX, double cameraY, double cameraZ) {
        checkThread();
        if (mesh == null) return;
        if (viewProjectionMatrix == null || viewProjectionMatrix.length != 16) throw new IllegalArgumentException("View-projection matrix must contain 16 floats");
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ))
            throw new IllegalArgumentException("Camera coordinates must be finite");
        if (device.indexCount(mesh.handle()) == 0) return;
        gl.useProgram(program);
        gl.uniformMatrix4(mvpLocation, false, viewProjectionMatrix);
        // Keep coordinates near the camera to reduce float precision loss in large worlds.
        gl.uniform3f(offsetLocation, (float) (mesh.chunkX() * 16.0 - cameraX), (float) -cameraY,
                (float) (mesh.chunkZ() * 16.0 - cameraZ));
        device.drawMesh(mesh.handle());
    }

    /** Compatibility overload for callers whose matrix already uses absolute world coordinates. */
    public void draw(GpuMesh mesh, float[] viewProjectionMatrix) {
        draw(mesh, viewProjectionMatrix, 0.0, 0.0, 0.0);
    }

    @Override public void close() {
        checkThread();
        gl.deleteProgram(program);
    }

    private int compile(int type, String source) {
        int shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.shaderCompileStatus(shader)) {
            String log = gl.shaderInfoLog(shader);
            gl.deleteShader(shader);
            throw new IllegalStateException("Voxel shader compile failed: " + log);
        }
        return shader;
    }

    private void checkThread() {
        Thread now = Thread.currentThread();
        if (renderThread == null) renderThread = now;
        else if (renderThread != now) throw new IllegalStateException("Renderer used outside render thread");
    }
}
