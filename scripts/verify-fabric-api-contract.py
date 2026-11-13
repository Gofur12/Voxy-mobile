#!/usr/bin/env python3
"""Guard the Fabric adapter against accidental API drift away from MC 1.20.1."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
build = (root / 'build.gradle').read_text(encoding='utf-8')
client = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileClient.java').read_text(encoding='utf-8')
context = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/MinecraftCameraFrame.java').read_text(encoding='utf-8')
chunk = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyChunkBridge.java').read_text(encoding='utf-8')
mixin = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/mixin/ClientWorldMixin.java').read_text(encoding='utf-8')
meta = (root / 'src/fabric/resources/fabric.mod.json').read_text(encoding='utf-8')
checks = {
    'Minecraft pinned to 1.20.1': "minecraft 'com.mojang:minecraft:1.20.1'" in build,
    'Yarn mappings pinned to 1.20.1': "mappings 'net.fabricmc:yarn:1.20.1+build.10:v2'" in build,
    'Fabric API pinned to 1.20.1': "fabric-api:0.92.2+1.20.1" in build,
    'metadata requires Minecraft 1.20.1': '"minecraft": "1.20.1"' in meta,
    'chunk load and unload events registered': 'ClientChunkEvents.CHUNK_LOAD.register' in client and 'ClientChunkEvents.CHUNK_UNLOAD.register' in client,
    'client tick event flushes deltas': 'ClientTickEvents.END_CLIENT_TICK.register' in client and 'VoxyChunkBridge.flushPendingChanges()' in client,
    'render hook uses 1.20.1 LAST event': 'WorldRenderEvents.LAST.register' in client,
    'disconnect event performs teardown': 'ClientPlayConnectionEvents.DISCONNECT.register' in client and 'VoxyRenderBridge.detach()' in client,
    'camera uses 1.20.1 matrixStack accessor': 'context.matrixStack()' in context and 'peek().getPositionMatrix()' in context,
    'camera uses projectionMatrix accessor': 'context.projectionMatrix()' in context,
    'camera position uses Camera.getPos': 'context.camera().getPos()' in context,
    'chunk bridge reads world build height': 'world.getBottomY()' in chunk and 'world.getHeight()' in chunk,
    'block state raw ID conversion is used': 'Block.getRawIdFromState(state)' in chunk,
    'mixin targets ClientWorld.setBlockState': '@Mixin(ClientWorld.class)' in mixin and 'method = "setBlockState"' in mixin,
    'mixin matches 1.20.1 setBlockState descriptor (two int args)': 'int flags,' in mixin and 'int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir' in mixin,
    'mixin forwards only successful state changes': 'Boolean.TRUE.equals(cir.getReturnValue())' in mixin,
    'no post-1.20.1 positionMatrix context accessor': 'context.positionMatrix()' not in context,
}
for label, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + label)
if not all(checks.values()):
    raise SystemExit(1)
print(f'Fabric 1.20.1 API contract checks: PASS ({len(checks)})')
