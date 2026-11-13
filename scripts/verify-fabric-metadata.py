#!/usr/bin/env python3
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
meta = json.loads((root / 'src/fabric/resources/fabric.mod.json').read_text())
assert meta['id'] == 'voxymobile'
assert meta['environment'] == 'client'
assert meta['entrypoints']['client'] == ['dev.brezze.voxymobile.fabric.VoxyMobileClient']
mixins = meta['mixins']
assert 'voxymobile.client.mixins.json' in mixins
mixin_cfg = json.loads((root / 'src/fabric/resources/voxymobile.client.mixins.json').read_text())
assert mixin_cfg['package'] == 'dev.brezze.voxymobile.fabric.mixin'
for cls in meta['entrypoints']['client']:
    rel = Path('src/fabric/java') / Path(*cls.split('.')).with_suffix('.java')
    assert (root / rel).is_file(), f'missing client entrypoint: {rel}'
for resource in ('assets/voxymobile/shaders/voxel_terrain.vsh', 'assets/voxymobile/shaders/voxel_terrain.fsh'):
    assert (root / 'src/main/resources' / resource).is_file(), f'missing shader: {resource}'
assert '${version}' in (root / 'src/fabric/resources/fabric.mod.json').read_text()
# Every resource source root must be included in Gradle's main resource set.
build_gradle = (root / 'build.gradle').read_text()
assert "'src/main/resources', 'src/fabric/resources'" in build_gradle, 'Gradle must package both main and Fabric resources'
assert "id 'fabric-loom' version '1.6.12'" in build_gradle, 'pin Loom to a stable 1.6 release'
import re
version_match = re.search(r"^version\s*=\s*'([^']+)'", build_gradle, re.MULTILINE)
assert version_match, 'project version must be declared in build.gradle'
assert meta['version'] == '${version}', 'fabric.mod.json must use Gradle version expansion'
assert 'options.release = 17' in build_gradle, 'compiled bytecode must target Java 17'
print('Fabric metadata/resource validation: PASS')
