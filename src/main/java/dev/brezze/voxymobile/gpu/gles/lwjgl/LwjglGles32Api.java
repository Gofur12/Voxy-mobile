package dev.brezze.voxymobile.gpu.gles.lwjgl;

import dev.brezze.voxymobile.gpu.gles.Gles32Api;
import dev.brezze.voxymobile.gpu.gles.GlesRuntimeInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reflection-backed adapter for the LWJGL OpenGL entry points used by Minecraft.
 * It deliberately does not create an EGL context or call GL.createCapabilities():
 * Zalith/MobileGlues must already have made the Minecraft render context current.
 */
public final class LwjglGles32Api implements Gles32Api {
    private static final int GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_LINK_STATUS = 0x8B82;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)");

    private final Class<?> gl11;
    private final Class<?> gl15;
    private final Class<?> gl20;
    private final Class<?> gl30;
    private final Class<?> memoryUtil;

    public LwjglGles32Api() {
        try {
            gl11 = Class.forName("org.lwjgl.opengl.GL11C");
            gl15 = Class.forName("org.lwjgl.opengl.GL15C");
            gl20 = Class.forName("org.lwjgl.opengl.GL20C");
            gl30 = Class.forName("org.lwjgl.opengl.GL30C");
            memoryUtil = Class.forName("org.lwjgl.system.MemoryUtil");
        } catch (ClassNotFoundException missing) {
            throw new IllegalStateException("LWJGL OpenGL classes are unavailable. Run inside Minecraft after Zalith has initialized its renderer.", missing);
        }
    }

    /** Query this only while Minecraft's GL context is current on the render thread. */
    public GlesRuntimeInfo queryRuntimeInfo() {
        String version = (String) call(gl11, "glGetString", new Class<?>[]{int.class}, GL_VERSION);
        String renderer = (String) call(gl11, "glGetString", new Class<?>[]{int.class}, GL_RENDERER);
        int attributes = (Integer) call(gl11, "glGetInteger", new Class<?>[]{int.class}, GL_MAX_VERTEX_ATTRIBS);
        Matcher matcher = VERSION.matcher(version == null ? "" : version);
        if (!matcher.find()) {
            throw new IllegalStateException("Cannot parse GL_VERSION returned by MobileGlues/LWJGL: " + version);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return new GlesRuntimeInfo(version, renderer, major, minor, attributes);
    }

    @Override public int createVertexArray() { return (Integer) call(gl30, "glGenVertexArrays", new Class<?>[0]); }
    @Override public void bindVertexArray(int vao) { call(gl30, "glBindVertexArray", new Class<?>[]{int.class}, vao); }
    @Override public int createBuffer() { return (Integer) call(gl15, "glGenBuffers", new Class<?>[0]); }
    @Override public void bindBuffer(int target, int buffer) { call(gl15, "glBindBuffer", new Class<?>[]{int.class, int.class}, target, buffer); }

    @Override public void uploadFloats(int target, float[] values, int usage) {
        if (values.length == 0) {
            call(gl15, "glBufferData", new Class<?>[]{int.class, long.class, int.class}, target, 0L, usage);
            return;
        }
        FloatBuffer buffer = (FloatBuffer) call(memoryUtil, "memAllocFloat", new Class<?>[]{int.class}, values.length);
        try {
            buffer.put(values).flip();
            call(gl15, "glBufferData", new Class<?>[]{int.class, FloatBuffer.class, int.class}, target, buffer, usage);
        } finally { call(memoryUtil, "memFree", new Class<?>[]{Buffer.class}, buffer); }
    }

    @Override public void uploadInts(int target, int[] values, int usage) {
        if (values.length == 0) {
            call(gl15, "glBufferData", new Class<?>[]{int.class, long.class, int.class}, target, 0L, usage);
            return;
        }
        IntBuffer buffer = (IntBuffer) call(memoryUtil, "memAllocInt", new Class<?>[]{int.class}, values.length);
        try {
            buffer.put(values).flip();
            call(gl15, "glBufferData", new Class<?>[]{int.class, IntBuffer.class, int.class}, target, buffer, usage);
        } finally { call(memoryUtil, "memFree", new Class<?>[]{Buffer.class}, buffer); }
    }

    @Override public void enableVertexAttribArray(int index) { call(gl20, "glEnableVertexAttribArray", new Class<?>[]{int.class}, index); }
    @Override public void vertexAttribPointer(int index, int size, int type, boolean normalized, int strideBytes, int offsetBytes) {
        call(gl20, "glVertexAttribPointer", new Class<?>[]{int.class, int.class, int.class, boolean.class, int.class, long.class}, index, size, type, normalized, strideBytes, (long) offsetBytes);
    }
    @Override public void deleteVertexArray(int vao) { call(gl30, "glDeleteVertexArrays", new Class<?>[]{int.class}, vao); }
    @Override public void deleteBuffer(int buffer) { call(gl15, "glDeleteBuffers", new Class<?>[]{int.class}, buffer); }
    @Override public int createShader(int type) { return (Integer) call(gl20, "glCreateShader", new Class<?>[]{int.class}, type); }
    @Override public void shaderSource(int shader, String source) { call(gl20, "glShaderSource", new Class<?>[]{int.class, CharSequence.class}, shader, source); }
    @Override public void compileShader(int shader) { call(gl20, "glCompileShader", new Class<?>[]{int.class}, shader); }
    @Override public boolean shaderCompileStatus(int shader) { return (Integer) call(gl20, "glGetShaderi", new Class<?>[]{int.class, int.class}, shader, GL_COMPILE_STATUS) != 0; }
    @Override public String shaderInfoLog(int shader) { return (String) call(gl20, "glGetShaderInfoLog", new Class<?>[]{int.class}, shader); }
    @Override public void deleteShader(int shader) { call(gl20, "glDeleteShader", new Class<?>[]{int.class}, shader); }
    @Override public int createProgram() { return (Integer) call(gl20, "glCreateProgram", new Class<?>[0]); }
    @Override public void attachShader(int program, int shader) { call(gl20, "glAttachShader", new Class<?>[]{int.class, int.class}, program, shader); }
    @Override public void bindAttribLocation(int program, int index, String name) { call(gl20, "glBindAttribLocation", new Class<?>[]{int.class, int.class, CharSequence.class}, program, index, name); }
    @Override public void linkProgram(int program) { call(gl20, "glLinkProgram", new Class<?>[]{int.class}, program); }
    @Override public boolean programLinkStatus(int program) { return (Integer) call(gl20, "glGetProgrami", new Class<?>[]{int.class, int.class}, program, GL_LINK_STATUS) != 0; }
    @Override public String programInfoLog(int program) { return (String) call(gl20, "glGetProgramInfoLog", new Class<?>[]{int.class}, program); }
    @Override public void deleteProgram(int program) { call(gl20, "glDeleteProgram", new Class<?>[]{int.class}, program); }
    @Override public void useProgram(int program) { call(gl20, "glUseProgram", new Class<?>[]{int.class}, program); }
    @Override public int getUniformLocation(int program, String name) { return (Integer) call(gl20, "glGetUniformLocation", new Class<?>[]{int.class, CharSequence.class}, program, name); }

    @Override public void uniformMatrix4(int location, boolean transpose, float[] matrix) {
        FloatBuffer buffer = (FloatBuffer) call(memoryUtil, "memAllocFloat", new Class<?>[]{int.class}, matrix.length);
        try {
            buffer.put(matrix).flip();
            call(gl20, "glUniformMatrix4fv", new Class<?>[]{int.class, boolean.class, FloatBuffer.class}, location, transpose, buffer);
        } finally { call(memoryUtil, "memFree", new Class<?>[]{Buffer.class}, buffer); }
    }

    @Override public void uniform3f(int location, float x, float y, float z) {
        call(gl20, "glUniform3f", new Class<?>[]{int.class, float.class, float.class, float.class}, location, x, y, z);
    }
    @Override public void drawElements(int mode, int count, int type, int offsetBytes) {
        call(gl11, "glDrawElements", new Class<?>[]{int.class, int.class, int.class, long.class}, mode, count, type, (long) offsetBytes);
    }

    private static Object call(Class<?> owner, String name, Class<?>[] signature, Object... args) {
        try {
            Method method = owner.getMethod(name, signature);
            return method.invoke(null, args);
        } catch (NoSuchMethodException | IllegalAccessException failure) {
            throw new IllegalStateException("LWJGL binding mismatch at " + owner.getName() + "." + name, failure);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("LWJGL call failed: " + name, cause);
        }
    }
}
