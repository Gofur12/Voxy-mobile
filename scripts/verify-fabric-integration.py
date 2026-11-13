#!/usr/bin/env python3
"""Static integration guardrails for the Fabric 1.20.1 adapter sources."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
chunk = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyChunkBridge.java').read_text()
bridge = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyRenderBridge.java').read_text()
client = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileClient.java').read_text()
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
    'block mutation mixin only forwards successful updates': 'Boolean.TRUE.equals(cir.getReturnValue())' in mixin,
}
for label, passed in checks.items():
    print(('PASS' if passed else 'FAIL') + ': ' + label)
if not all(checks.values()):
    raise SystemExit(1)
print(f'Fabric integration static checks: PASS ({len(checks)})')
