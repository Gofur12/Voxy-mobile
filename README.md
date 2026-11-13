# Voxy Mobile — Stage 29: Camera-relative distant LOD projection

Experimental client-side terrain LOD prototype for Minecraft Java 1.20.1 Fabric, targeting Android Zalith Launcher with MobileGlues / OpenGL ES 3.2.

## Stage 29 changes

- Replaces the inherited Minecraft/Iris projection matrix with a camera-relative LOD projection using a 2048-block far plane.
- Builds the view rotation from the active Minecraft camera quaternion; tile translations remain camera-relative to preserve float precision.
- Rejects transient invalid camera frames without tearing down and recreating the GLES backend.
- Clamps LOD fragment depth to the far plane. LOD therefore fills sky pixels while remaining behind nearer vanilla terrain and entities under the existing depth test.
- Keeps the 128-chunk retained history, bounded tile cache, and GLES 3.2 shader path.

## Build

Run GitHub Actions workflow **Fabric build**. It builds with Java 17 and uploads `VoxyMobileCore-0.1.0-stage29.jar` as the artifact `VoxyMobileCore-stage29-fabric-1.20.1`.

Install only that JAR in the Minecraft `mods` folder. Do not install the sources JAR. The source ZIP itself is not an installable mod.

## Runtime requirements

- Minecraft 1.20.1
- Fabric Loader >= 0.15.0
- Fabric API 0.92.2+1.20.1
- Java 17 or newer (Java 21 is compatible)
- OpenGL ES 3.2-compatible backend (MobileGlues on the intended Android target)

This remains an experimental heightfield LOD prototype, not a full Voxy-equivalent voxel renderer. It has not been device-tested by the project authoring environment; visual validation on the target device is still required.
