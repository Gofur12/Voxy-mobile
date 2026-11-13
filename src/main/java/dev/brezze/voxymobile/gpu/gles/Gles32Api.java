package dev.brezze.voxymobile.gpu.gles;

/** Minimal GLES 3.2 surface used by VoxyMobile. Implement this with the launcher's GLES binding. */
public interface Gles32Api {
    int GL_ARRAY_BUFFER = 0x8892;
    int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    int GL_STATIC_DRAW = 0x88E4;
    int GL_FLOAT = 0x1406;
    int GL_UNSIGNED_INT = 0x1405;
    int GL_TRIANGLES = 0x0004;
    int GL_VERTEX_SHADER = 0x8B31;
    int GL_FRAGMENT_SHADER = 0x8B30;

    int createVertexArray();
    void bindVertexArray(int vao);
    int createBuffer();
    void bindBuffer(int target, int buffer);
    void uploadFloats(int target, float[] values, int usage);
    void uploadInts(int target, int[] values, int usage);
    void enableVertexAttribArray(int index);
    void vertexAttribPointer(int index, int size, int type, boolean normalized, int strideBytes, int offsetBytes);
    void deleteVertexArray(int vao);
    void deleteBuffer(int buffer);
    int createShader(int type);
    void shaderSource(int shader, String source);
    void compileShader(int shader);
    boolean shaderCompileStatus(int shader);
    String shaderInfoLog(int shader);
    void deleteShader(int shader);
    int createProgram();
    void attachShader(int program, int shader);
    void bindAttribLocation(int program, int index, String name);
    void linkProgram(int program);
    boolean programLinkStatus(int program);
    String programInfoLog(int program);
    void deleteProgram(int program);
    void useProgram(int program);
    int getUniformLocation(int program, String name);
    void uniformMatrix4(int location, boolean transpose, float[] matrix);
    void uniform3f(int location, float x, float y, float z);
    void drawElements(int mode, int count, int type, int offsetBytes);
}
