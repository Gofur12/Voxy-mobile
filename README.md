# Voxy Mobile — Stage 28: First integrated heightfield LOD path

Experimental standalone terrain LOD renderer for Minecraft Java 1.20.1 + Fabric, targeting Android Zalith Launcher, MobileGlues, OpenGL ES 3.2 and Adreno-class hardware.

## Stage 28 changes

- Wired the existing quadtree `LodSelector` and `TerrainLodMesher` into the actual Fabric world-render callback. Stage 27 contained these core classes but never called them from the live renderer.
- Added a bounded 128-chunk snapshot history. Chunk data remains available for distant LOD after vanilla unloads a chunk, and is cleared on world/dimension identity changes.
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

Expected JAR: `build/libs/VoxyMobileCore-0.1.0-stage28.jar`. GitHub Actions uploads this JAR as an artifact. The source ZIP itself is not an installable mod.

## Validation status

- Dependency-free core smoke tests and static integration checks: PASS in the packaging environment.
- Fabric/Loom compilation and remapped JAR verification: NOT RUN in this environment. Run the included GitHub Actions workflow to compile against the actual 1.20.1 Fabric API.
- POCO X3 Pro / Zalith / MobileGlues in-game test: NOT RUN.

## Known limitations

- This is the first live heightfield LOD path, not feature parity with Voxy or Distant Horizons. It uses a bounded history of chunks the player has actually visited; it cannot invent terrain for never-loaded chunks.
- Height sampling uses the highest non-air block, so forests, tall structures, water and caves can produce imperfect silhouettes.
- LOD uses a stable terrain tint rather than biome-aware block textures, lighting, fluids or foliage.
- Heightfield mesh generation currently happens on the render thread when a tile is first needed; later work should move this build to a bounded worker queue.
- Frustum culling, seamless vanilla handoff, persistent disk cache, settings GUI and detailed block/material shading are not yet implemented.
- The shader and GL-state interaction still require an in-game GLES 3.2 validation pass.
