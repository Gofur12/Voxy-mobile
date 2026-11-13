#!/usr/bin/env python3
"""Guard the pinned, checksum-verified Gradle bootstrap and cross-platform build path."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
props = (root / 'gradle/wrapper/gradle-wrapper.properties').read_text(encoding='utf-8')
sh = (root / 'gradlew').read_text(encoding='utf-8')
bat = (root / 'gradlew.bat').read_text(encoding='utf-8')
build = (root / 'build.gradle').read_text(encoding='utf-8')
script = (root / 'scripts/build-fabric.sh').read_text(encoding='utf-8')
workflow = (root / '.github/workflows/fabric-build.yml').read_text(encoding='utf-8')
checks = {
    'Gradle distribution pinned to 8.6': 'gradle-8.6-bin.zip' in props,
    'official Gradle distribution SHA-256 pinned': '9631d53cf3e74bfa726893aee1f8994fee4e060c401335946dba2156f440f24c' in props,
    'POSIX bootstrap verifies checksum before extraction': sh.index('if [ "$ACTUAL" != "$SHA256" ]') < sh.index('unzip -q -o "$ARCHIVE"'),
    'POSIX bootstrap downloads to a temporary file before promotion': 'TEMP="$ARCHIVE.part"' in sh and 'mv "$TEMP" "$ARCHIVE"' in sh,
    'Windows bootstrap verifies checksum before extraction': bat.index('if /i not "%ACTUAL%"=="%SHA256%"') < bat.index('Expand-Archive'),
    'Gradle JAR validation uses built-in JVM ZIP APIs': 'java.util.zip.ZipFile' in build,
    'Gradle JAR validation checks Fabric metadata and runtime files': 'voxymobile.client.mixins.json' in build and 'voxel_terrain.vsh' in build and 'metadata.version' in build,
    'Gradle check depends on JAR verification': "dependsOn tasks.named('verifyFabricJar')" in build,
    'build script accepts the bootstrapped Gradle executable': '"$GRADLE" --no-daemon clean build' in script,
    'GitHub Actions workflow runs the real Loom build': 'gradle --no-daemon clean build' in workflow,
    'GitHub Actions workflow uploads the exact stage JAR': 'VoxyMobileCore-0.1.0-stage27.jar' in workflow,
}
for label, ok in checks.items():
    print(('PASS: ' if ok else 'FAIL: ') + label)
if not all(checks.values()):
    raise SystemExit('Gradle bootstrap/build path validation failed')
print(f'Gradle bootstrap/build path validation: PASS ({len(checks)})')
