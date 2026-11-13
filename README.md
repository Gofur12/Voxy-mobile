# Voxy Mobile — Stage 26: Cross-platform Fabric build bootstrap

Experimental standalone voxel-terrain renderer for Minecraft Java 1.20.1 + Fabric, targeting Android Zalith Launcher, MobileGlues, OpenGL ES 3.2 and Adreno-class hardware.

## Stage 26 changes

- Updated the mod version to `0.1.0-stage26`.
- Added world-identity tracking to the Minecraft chunk bridge. A world/dimension swap now clears old snapshots and pending block deltas.
- Invalidates in-flight and resident chunk meshes from the previous world before accepting snapshots from the new world.
- Preserves monotonically increasing per-chunk revisions across world swaps so stale worker results cannot overwrite new-world meshes.
- Added a bounded 2-second retry interval when the GLES backend cannot initialize on the first world-render callback. This avoids permanently disabling rendering after a transient context/binding failure and avoids retrying every frame.
- Added `scripts/verify-fabric-integration.py` and wired it into `scripts/verify-core.sh`.

## Validation

Run all dependency-free checks, including the session lifecycle regression test:

```sh
./scripts/verify-all.sh
```

Check build prerequisites first, then build the actual Fabric mod (requires Gradle 8.6+ and network access for first-time dependency downloads):

```sh
./scripts/preflight.sh
./scripts/build-fabric.sh
```

Expected output JAR:

```text
build/libs/VoxyMobileCore-0.1.0-stage26.jar
```

## Stage 25 verification status

- 14 dependency-free Java smoke-test entrypoints: PASS.
- Fabric metadata/resource validation: PASS.
- 9 static Fabric integration guardrails: PASS.
- 16 Fabric 1.20.1 API contract checks: PASS.
- 7 client session lifecycle regression checks: PASS.
- 4 render failure recovery checks: PASS.
- ZIP integrity: checked when packaging.
- Actual Loom compilation and remapped JAR verification: NOT RUN. Gradle is not installed in the build environment and outbound DNS/network access is unavailable, so no installable JAR is claimed.
- POCO X3 Pro / Zalith / MobileGlues in-game test: NOT RUN.

## Runtime limitations — do not mistake source validation for in-game validation

- Fabric adapter classes have not yet been compiled against the downloaded Minecraft/Fabric dependencies in this environment.
- GL binding, shader compilation, camera matrix convention, depth/state interaction with vanilla rendering, and actual terrain visibility still require an in-game test.
- This remains an experimental terrain renderer. It does not yet implement full block model geometry, fluids, transparency, biome tint, vanilla lighting/lightmap, or seamless replacement of vanilla terrain rendering.


### Stage 25 reliability and artifact checks

- A runtime exception or linkage failure during a world-render frame now detaches the failing backend on the render thread, attempts cleanup, and schedules a bounded retry. This prevents renderer-side failures from propagating directly through the world-render callback.
- `scripts/build-fabric.sh` prefers `./gradlew` when supplied, otherwise uses a system Gradle installation, and enforces Gradle 8.6 or newer.
- The Gradle `check` lifecycle now depends on `verifyFabricJar`, which validates the remapped JAR metadata, Fabric entrypoints/mixin classes, and both shader resources.
- The JAR verifier derives the expected mod version from `build.gradle`, avoiding stale hard-coded stage numbers.

Build verification still requires a working Gradle installation and network access to resolve Minecraft, Fabric, and Loom dependencies. Core smoke tests do not substitute for a Fabric/Loom build or a device test.


### Stage 25 build preflight

- Added `scripts/preflight.sh` to distinguish a valid Java 17+ environment from a missing Gradle installation before attempting a build.
- Added a Gradle `verifyJavaRuntime` task and wired it into `check`.
- Build script derives the expected JAR filename from `settings.gradle`.
- No Gradle Wrapper executable is bundled: wrapper properties alone are insufficient to launch Gradle, and the wrapper JAR/distribution could not be fetched in this network-restricted environment.

### Stage 25 — Fabric 1.20.1 API contract hardening

- Added `scripts/verify-fabric-api-contract.py` to guard the adapter's declared Minecraft/Yarn/Fabric API versions and the exact Fabric hooks/accessors used by the client bridge.
- Added checks for the 1.20.1 `WorldRenderContext` camera, projection, and matrix-stack access pattern; chunk lifecycle events; tick delta flushing; disconnect teardown; and the `ClientWorld#setBlockState` mixin target.
- Added the API contract verifier to the dependency-free verification pipeline.
- Updated the artifact version to `0.1.0-stage26`.

This is a source/API contract check, not a substitute for compiling against Loom's resolved Minecraft and Fabric dependencies. The actual Fabric build and Zalith device test remain unverified until Gradle and its dependencies can be resolved.


## Stage 25 session lifecycle fix

The client render bridge now explicitly re-enables itself on Fabric's client connection JOIN event. This is required because disconnect teardown disables rendering; without a matching JOIN transition, a later multiplayer session could remain permanently render-disabled even though the backend retry path is healthy. Run `sh scripts/verify-all.sh` for the dependency-free source and lifecycle checks. These checks do not replace a Loom compile or an in-game test.


## Stage 26 — build path improvements

- Added a POSIX `./gradlew` bootstrap and Windows `gradlew.bat`. Both download the pinned Gradle 8.6 binary distribution and verify its official SHA-256 before extraction or execution. The scripts are lightweight bootstrappers, not the official Gradle Wrapper JAR implementation.
- Added `gradle/wrapper/gradle-wrapper.properties` with the Gradle 8.6 distribution URL and checksum.
- Removed the Python dependency from Gradle's `verifyFabricJar` task. JAR entries and `fabric.mod.json` are now checked using the JVM's ZIP and JSON APIs, so the Gradle verification task works on Windows as well as Linux.
- Updated `scripts/build-fabric.sh` to use whichever Gradle launcher it selected for both build and JAR verification.
- Added a GitHub Actions workflow that runs the dependency-free checks, performs the actual Loom build, and uploads the Stage 26 JAR as a workflow artifact.
- Added 10 bootstrap/build-path regression checks.

### Build commands

Linux/macOS: `./gradlew --no-daemon clean build`

Windows: `gradlew.bat --no-daemon clean build`

First run requires internet access to download Gradle 8.6 and resolve Fabric/Minecraft/Loom dependencies. The Gradle distribution is checksum-verified against the official Gradle checksum reference.

### Stage 26 verification status

- Dependency-free smoke and static checks: PASS.
- POSIX and Windows bootstrap scripts: syntax/static validation PASS.
- Actual Gradle bootstrap/build in this environment: BLOCKED because DNS cannot resolve `services.gradle.org`; the downloaded distribution and Loom dependencies are therefore unavailable.
- Fabric/Loom compilation, remapped JAR verification, and POCO/Zalith runtime test: NOT RUN. No installable JAR is included in this source ZIP.
