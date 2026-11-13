# Voxy Mobile — Stage 30: Camera-space and LOD-radius fix

Experimental standalone terrain LOD renderer for Minecraft Java 1.20.1 + Fabric, targeting Android Zalith Launcher, MobileGlues, OpenGL ES 3.2 and Adreno-class hardware.

## Stage 30 changes

- Added a touch-friendly in-game settings screen, opened with **O**.
- Added persistent ON/OFF controls for the Voxy Mobile render hook and distant LOD.
- Added selectable LOD radius (8 / 16 / 24 chunks), Reset default and Done buttons.
- Settings are saved to `config/voxymobile.properties`.

- Wired the existing quadtree `LodSelector` and `TerrainLodMesher` into the actual Fabric world-render callback. Stage 27 contained these core classes but never called them from the live renderer.
- Fixed camera-space composition: retain view rotation but remove the camera translation before applying camera-relative tile offsets, preventing the camera position from being subtracted twice.
- Skip transient non-finite camera/projection frames instead of tearing down a healthy GLES backend.
- Ensure the LOD search radius extends at least eight chunks beyond the vanilla view distance (up to the 32-chunk mobile cap), so a distant ring can actually exist.
- Increased the bounded visited-chunk snapshot history from 128 to 256 chunks (higher memory use, still bounded).
- Added regression checks for camera translation, transient matrices and LOD radius.
- Added a bounded 256-chunk snapshot history. Chunk data remains available for distant LOD after vanilla unloads a chunk, and is cleared on world/dimension identity changes.
- Added quadtree tile rendering with smaller-tile fallback when the retained history does not fully cover a coarse tile.
- Builds sampled heightfield tiles and draws them only outside the current vanilla view-distance boundary.
- Removed the Stage 27 full voxel-mesh overlay from the live backend. Its pseudo-random material-ID color hash caused the conspicuous pink/red/yellow block overlay and was not a valid LOD implementation.
- Replaced the random hash fragment color with a stable terrain tint for this initial heightfield pass.
- Keeps GPU mesh creation, upload and deletion on the render thread.

## Build

Run dependency-free checks:

```sh
./scripts/verify-all.sh
```

Build the installable Fabric JAR (requires Gradle 8.6+ and network access to resolve Minecraft/Fabric/Loom):

```sh
gradle --no-daemon clean build
```

Expected JAR: `build/libs/VoxyMobileCore-0.1.0-stage30.jar`. GitHub Actions uploads this JAR as an artifact. The source ZIP itself is not an installable mod.

## Validation status

- Dependency-free core smoke tests and static integration checks: PASS in the packaging environment.
- Fabric/Loom compilation and remapped JAR verification: NOT RUN; Gradle 8.6 could not be downloaded because this environment has no network access. Run the included GitHub Actions workflow to compile against the actual 1.20.1 Fabric API.
- POCO X3 Pro / Zalith / MobileGlues in-game test: NOT RUN. This is a source ZIP, not a tested installable JAR.

## Known limitations

- This is the first live heightfield LOD path, not feature parity with Voxy or Distant Horizons. It uses a bounded 256-chunk history of chunks the player has actually visited; it cannot invent terrain for never-loaded chunks.
- Height sampling uses the highest non-air block, so forests, tall structures, water and caves can produce imperfect silhouettes.
- LOD uses a stable terrain tint rather than biome-aware block textures, lighting, fluids or foliage.
- Heightfield mesh generation currently happens on the render thread when a tile is first needed; later work should move this build to a bounded worker queue.
- Frustum culling, seamless vanilla handoff, persistent disk cache, detailed block/material shading is not yet implemented.
- The shader and GL-state interaction still require an in-game GLES 3.2 validation pass.
