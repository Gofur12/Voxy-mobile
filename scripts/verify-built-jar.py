#!/usr/bin/env python3
"""Validate the minimum installable Fabric JAR contents without launching Minecraft."""
import json
import sys
import re
import zipfile
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-built-jar.py <mod.jar>')
jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f'missing JAR: {jar}')
required = {
    'fabric.mod.json',
    'voxymobile.client.mixins.json',
    'assets/voxymobile/shaders/voxel_terrain.vsh',
    'assets/voxymobile/shaders/voxel_terrain.fsh',
    'dev/brezze/voxymobile/fabric/VoxyMobileClient.class',
    'dev/brezze/voxymobile/fabric/mixin/ClientWorldMixin.class',
}
with zipfile.ZipFile(jar) as archive:
    names = set(archive.namelist())
    missing = sorted(required - names)
    if missing:
        raise SystemExit('JAR validation failed; missing entries:\n' + '\n'.join(missing))
    metadata = json.loads(archive.read('fabric.mod.json'))
    if metadata.get('id') != 'voxymobile':
        raise SystemExit('JAR metadata has unexpected mod id')
    build_gradle = Path('build.gradle').read_text(encoding='utf-8')
    match = re.search(r"^version\s*=\s*'([^']+)'", build_gradle, re.MULTILINE)
    if not match:
        raise SystemExit('could not determine expected project version from build.gradle')
    if metadata.get('version') != match.group(1):
        raise SystemExit(f"JAR metadata has unexpected version: {metadata.get('version')} (expected {match.group(1)})")
print(f'Built JAR validation: PASS ({jar.stat().st_size} bytes)')
