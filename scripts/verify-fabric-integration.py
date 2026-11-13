#!/usr/bin/env python3
"""Static integration guardrails for the Fabric 1.20.1 adapter sources."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
chunk = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyChunkBridge.java').read_text()
bridge = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyRenderBridge.java').read_text()
client = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileClient.java').read_text()
lod = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/LodHeightfieldRenderer.java').read_text()
backend = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/LwjglVoxyRenderBackend.java').read_text()
mixin = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/mixin/ClientWorldMixin.java').read_text()
checks = {
    'world identity reset on chunk load': 'ensureWorld(world);' in chunk.split('public static void onChunkLoad', 1)[1].split('public static void onChunkUnload', 1)[0],
    'world identity reset on chunk unload': 'ensureWorld(world);' in chunk.split('public static void onChunkUnload', 1)[1].split('public static void onBlockChanged', 1)[0],
    'world identity reset on block changes': 'ensureWorld(world);' in chunk.split('public static void onBlockChanged', 1)[1].split('public static void flushPendingChanges', 1)[0],
    'old meshes invalidated on world swap': 'runtime.unload(snapshot.chunkX(), snapshot.chunkZ(), nextRevision(key));' in chunk,
    'monotonic revisions preserved across world swap': 'Keep revisions monotonic' in chunk and 'REVISIONS.clear();' in chunk.split('private static void ensureWorld', 1)[0],
    'backend initialization has bounded retry delay': 'CREATE_RETRY_DELAY_NANOS = 2_000_000_000L' in bridge and 'now >= nextCreateAttemptNanos' in bridge,
    'retry timestamp reset on detach': 'nextCreateAttemptNanos = 0L;' in bridge,
    'chunk lifecycle events registered': 'ClientChunkEvents.CHUNK_LOAD.register' in client and 'ClientChunkEvents.CHUNK_UNLOAD.register' in client,
    'block mutation mixin matches two-int target descriptor': 'int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir' in mixin,
    'block mutation mixin only forwards successful updates': 'Boolean.TRUE.equals(cir.getReturnValue())' in mixin,
    'bounded 256-chunk history survives vanilla unload': 'MAX_LOD_HISTORY_CHUNKS = 256' in chunk and 'rememberForLod(key, snapshot)' in chunk,
    'LOD selector is wired into live renderer': 'selector.select(cameraX, cameraZ' in lod and 'TerrainLodMesher.build' in lod,
    'LOD renderer draws retained snapshots': 'VoxyChunkBridge.getLodSnapshots()' in backend and 'lodRenderer.render' in backend,
    'LOD falls back to smaller tiles for sparse history': 'drawWithFallback' in lod and 'tile.level - 1' in lod,
    'LOD radius extends beyond vanilla view distance': 'Math.max(configuredRadius, vanillaDistance + 8)' in lod,
    'camera translation is not applied twice': 'viewRotation.m30(0.0f).m31(0.0f).m32(0.0f)' in (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/MinecraftCameraFrame.java').read_text(),
    'invalid transient camera matrices skip frame without backend teardown': 'if (camera != null) current.renderFrame(context, camera);' in bridge,
}
for label, passed in checks.items():
    print(('PASS' if passed else 'FAIL') + ': ' + label)
if not all(checks.values()):
    raise SystemExit(1)
print(f'Fabric integration static checks: PASS ({len(checks)})')

# Stage 29 in-game configuration surface and live setting wiring.
client = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileClient.java').read_text(encoding='utf-8')
screen = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileConfigScreen.java').read_text(encoding='utf-8')
config = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileConfig.java').read_text(encoding='utf-8')
backend = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/LwjglVoxyRenderBackend.java').read_text(encoding='utf-8')
assert 'GLFW_KEY_O' in client and 'new VoxyMobileConfigScreen' in client
assert 'Voxy Mobile: ' in screen and 'Distant LOD: ' in screen and 'Reset default' in screen
assert 'voxymobile.properties' in config and 'radiusChunks' in config
assert 'VoxyMobileConfig.lodEnabled' in backend
print('PASS: settings GUI, O keybind, persistent toggles and LOD wiring')
