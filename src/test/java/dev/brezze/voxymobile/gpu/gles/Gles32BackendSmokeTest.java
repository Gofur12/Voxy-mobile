package dev.brezze.voxymobile.gpu.gles;

import java.util.HashMap;
import java.util.Map;

public final class Gles32BackendSmokeTest {
    public static void main(String[] args) {
        FakeGles gl = new FakeGles();
        OpenGles32GpuDevice device = new OpenGles32GpuDevice(gl);
        int handle = device.createMesh(new float[]{0,0,0,4, 1,0,0,4, 1,1,0,4, 0,1,0,4}, new int[]{0,1,2,0,2,3});
        check(device.liveMeshCount() == 1, "mesh allocation");
        check(device.indexCount(handle) == 6, "index count");
        VoxelTerrainRenderer renderer = new VoxelTerrainRenderer(gl, device, "vertex", "fragment");
        renderer.draw(new dev.brezze.voxymobile.gpu.GpuMesh(handle, 2, -3, 1, 100), identity());
        check(gl.drawCalls == 1 && gl.lastDrawCount == 6, "indexed draw");
        device.deleteMesh(handle);
        check(device.liveMeshCount() == 0 && gl.deletedBuffers == 2 && gl.deletedVaos == 1, "resource deletion");
        renderer.close();
        check(gl.deletedPrograms == 1, "shader program deletion");
        System.out.println("Gles32BackendSmokeTest: PASS");
    }
    private static float[] identity() { return new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1}; }
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }

    private static final class FakeGles implements Gles32Api {
        int next = 1, drawCalls, lastDrawCount, deletedBuffers, deletedVaos, deletedPrograms;
        final Map<Integer, String> sources = new HashMap<>();
        public int createVertexArray(){return next++;} public void bindVertexArray(int v){}
        public int createBuffer(){return next++;} public void bindBuffer(int t,int b){}
        public void uploadFloats(int t,float[] v,int u){} public void uploadInts(int t,int[] v,int u){}
        public void enableVertexAttribArray(int i){} public void vertexAttribPointer(int i,int s,int t,boolean n,int st,int o){}
        public void deleteVertexArray(int v){deletedVaos++;} public void deleteBuffer(int b){deletedBuffers++;}
        public int createShader(int t){return next++;} public void shaderSource(int s,String v){sources.put(s,v);} public void compileShader(int s){}
        public boolean shaderCompileStatus(int s){return true;} public String shaderInfoLog(int s){return "";} public void deleteShader(int s){}
        public int createProgram(){return next++;} public void attachShader(int p,int s){} public void bindAttribLocation(int p,int i,String n){}
        public void linkProgram(int p){} public boolean programLinkStatus(int p){return true;} public String programInfoLog(int p){return "";}
        public void deleteProgram(int p){deletedPrograms++;} public void useProgram(int p){} public int getUniformLocation(int p,String n){return n.equals("uMvp")?1:2;}
        public void uniformMatrix4(int l,boolean t,float[] m){} public void uniform3f(int l,float x,float y,float z){}
        public void drawElements(int m,int c,int t,int o){drawCalls++;lastDrawCount=c;}
    }
}
