package dev.brezze.voxymobile.render;

import dev.brezze.voxymobile.gpu.GpuDevice;
import dev.brezze.voxymobile.gpu.GpuMesh;
import dev.brezze.voxymobile.gpu.gles.Gles32Api;
import dev.brezze.voxymobile.gpu.gles.OpenGles32GpuDevice;
import dev.brezze.voxymobile.gpu.gles.VoxelTerrainRenderer;
import dev.brezze.voxymobile.android.AndroidPerformanceProfile;
import dev.brezze.voxymobile.ingest.ChunkBlockSource;
import dev.brezze.voxymobile.ingest.ChunkIngestor;
import dev.brezze.voxymobile.mesh.VoxyMobileRuntime;
import dev.brezze.voxymobile.scheduler.ChunkPos;
import java.util.List;

public final class TerrainFrameRendererSmokeTest {
    public static void main(String[] args) throws Exception {
        FakeGl gl = new FakeGl();
        OpenGles32GpuDevice gpu = new OpenGles32GpuDevice(gl);
        AndroidPerformanceProfile profile = AndroidPerformanceProfile.conservative(8, 256L << 20);
        VoxyMobileRuntime runtime = new VoxyMobileRuntime(gpu, profile, 8, 16, 4L << 20);
        VoxelTerrainRenderer renderer = new VoxelTerrainRenderer(gl, gpu, "vertex", "fragment");
        TerrainFrameRenderer frame = new TerrainFrameRenderer(runtime, renderer);
        ChunkBlockSource source = new ChunkBlockSource() {
            public int minY() { return 0; }
            public int height() { return 2; }
            public int materialId(int x, int y, int z) { return y == 0 ? 1 : 0; }
        };
        runtime.submit(ChunkIngestor.ingest(0, 0, 1, source));
        long until = System.currentTimeMillis() + 3000;
        TerrainFrameStats stats = null;
        while (System.currentTimeMillis() < until) {
            stats = frame.render(new ChunkRenderList(List.of(new ChunkPos(0, 0))), identity(), 8, 20, 8, 16.0);
            if (stats.drawn == 1) break;
            Thread.sleep(10);
        }
        if (stats == null || stats.drawn != 1 || gl.draws != 1) throw new AssertionError("expected one resident chunk draw");
        if (gl.lastOffset == null || Math.abs(gl.lastOffset[0] + 8f) > 0.001f || Math.abs(gl.lastOffset[1] + 20f) > 0.001f || Math.abs(gl.lastOffset[2] + 8f) > 0.001f)
            throw new AssertionError("camera-relative offset incorrect");
        renderer.close();
        gpu.deleteAllMeshes();
        runtime.close();
        System.out.println("TerrainFrameRendererSmokeTest: PASS");
    }
    private static float[] identity() { return new float[]{1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1}; }

    private static final class FakeGl implements Gles32Api {
        int id=1, draws; float[] lastOffset;
        public int createVertexArray(){return id++;} public void bindVertexArray(int v){} public int createBuffer(){return id++;}
        public void bindBuffer(int t,int b){} public void uploadFloats(int t,float[] v,int u){} public void uploadInts(int t,int[] v,int u){}
        public void enableVertexAttribArray(int i){} public void vertexAttribPointer(int i,int s,int t,boolean n,int st,int o){}
        public void deleteVertexArray(int v){} public void deleteBuffer(int b){} public int createShader(int t){return id++;}
        public void shaderSource(int s,String x){} public void compileShader(int s){} public boolean shaderCompileStatus(int s){return true;}
        public String shaderInfoLog(int s){return "";} public void deleteShader(int s){} public int createProgram(){return id++;}
        public void attachShader(int p,int s){} public void bindAttribLocation(int p,int i,String n){} public void linkProgram(int p){}
        public boolean programLinkStatus(int p){return true;} public String programInfoLog(int p){return "";} public void deleteProgram(int p){}
        public void useProgram(int p){} public int getUniformLocation(int p,String n){return n.equals("uMvp")?1:2;}
        public void uniformMatrix4(int l,boolean t,float[] m){} public void uniform3f(int l,float x,float y,float z){lastOffset=new float[]{x,y,z};}
        public void drawElements(int m,int c,int t,int o){draws++;}
    }
}
