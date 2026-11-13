package dev.brezze.voxymobile.gpu.gles;

import dev.brezze.voxymobile.gpu.GpuDevice;
import java.util.HashMap;
import java.util.Map;

/** GLES 3.2 VBO/IBO uploader. Every method is render-thread-only. */
public final class OpenGles32GpuDevice implements GpuDevice {
    private static final int STRIDE_BYTES = 4 * Float.BYTES;
    private final Gles32Api gl;
    private final Map<Integer, MeshObjects> meshes = new HashMap<>();
    private Thread renderThread;
    private int nextHandle = 1;

    public OpenGles32GpuDevice(Gles32Api gl) {
        if (gl == null) throw new NullPointerException("gl");
        this.gl = gl;
    }

    @Override public int createMesh(float[] vertices, int[] indices) {
        checkThread();
        if (vertices == null || indices == null) throw new NullPointerException("mesh arrays");
        if (vertices.length % 4 != 0 || indices.length % 3 != 0) throw new IllegalArgumentException("invalid voxel mesh layout");
        int vao = gl.createVertexArray();
        int vbo = gl.createBuffer();
        int ebo = gl.createBuffer();
        try {
            gl.bindVertexArray(vao);
            gl.bindBuffer(Gles32Api.GL_ARRAY_BUFFER, vbo);
            gl.uploadFloats(Gles32Api.GL_ARRAY_BUFFER, vertices, Gles32Api.GL_STATIC_DRAW);
            gl.bindBuffer(Gles32Api.GL_ELEMENT_ARRAY_BUFFER, ebo);
            gl.uploadInts(Gles32Api.GL_ELEMENT_ARRAY_BUFFER, indices, Gles32Api.GL_STATIC_DRAW);
            gl.enableVertexAttribArray(0);
            gl.vertexAttribPointer(0, 3, Gles32Api.GL_FLOAT, false, STRIDE_BYTES, 0);
            gl.enableVertexAttribArray(1);
            gl.vertexAttribPointer(1, 1, Gles32Api.GL_FLOAT, false, STRIDE_BYTES, 3 * Float.BYTES);
            gl.bindVertexArray(0);
            int handle = nextHandle++;
            if (handle == 0) handle = nextHandle++;
            meshes.put(handle, new MeshObjects(vao, vbo, ebo, indices.length));
            return handle;
        } catch (RuntimeException | Error failure) {
            gl.bindVertexArray(0);
            gl.deleteBuffer(ebo);
            gl.deleteBuffer(vbo);
            gl.deleteVertexArray(vao);
            throw failure;
        }
    }

    @Override public void deleteMesh(int handle) {
        checkThread();
        MeshObjects mesh = meshes.remove(handle);
        if (mesh == null) return;
        gl.deleteBuffer(mesh.ebo);
        gl.deleteBuffer(mesh.vbo);
        gl.deleteVertexArray(mesh.vao);
    }

    public void drawMesh(int handle) {
        checkThread();
        MeshObjects mesh = meshes.get(handle);
        if (mesh == null) return;
        if (mesh.indexCount == 0) return;
        gl.bindVertexArray(mesh.vao);
        gl.drawElements(Gles32Api.GL_TRIANGLES, mesh.indexCount, Gles32Api.GL_UNSIGNED_INT, 0);
        gl.bindVertexArray(0);
    }

    public int indexCount(int handle) {
        checkThread();
        MeshObjects mesh = meshes.get(handle);
        return mesh == null ? 0 : mesh.indexCount;
    }

    public int liveMeshCount() { checkThread(); return meshes.size(); }

    /** Deletes every VBO/EBO/VAO owned by this device. Call before the GL context is destroyed. */
    public void deleteAllMeshes() {
        checkThread();
        for (MeshObjects mesh : meshes.values()) {
            gl.deleteBuffer(mesh.ebo);
            gl.deleteBuffer(mesh.vbo);
            gl.deleteVertexArray(mesh.vao);
        }
        meshes.clear();
    }

    private void checkThread() {
        Thread now = Thread.currentThread();
        if (renderThread == null) renderThread = now;
        else if (renderThread != now) throw new IllegalStateException("GLES call outside render thread");
    }

    private static final class MeshObjects {
        final int vao, vbo, ebo, indexCount;
        MeshObjects(int vao, int vbo, int ebo, int indexCount) {
            this.vao = vao; this.vbo = vbo; this.ebo = ebo; this.indexCount = indexCount;
        }
    }
}
