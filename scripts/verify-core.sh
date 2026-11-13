#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
rm -rf build/verify-classes
mkdir -p build/verify-classes
javac --release 17 -d build/verify-classes $(find src/main/java src/test/java -name '*.java' -print)
count=0
find src/test/java -name '*SmokeTest.java' -print | sort | while IFS= read -r file; do
  cls=${file#src/test/java/}
  cls=${cls%.java}
  cls=$(printf '%s' "$cls" | tr '/' '.')
  java -cp build/verify-classes "$cls"
done
python3 scripts/verify-fabric-metadata.py
python3 scripts/verify-fabric-integration.py
python3 scripts/verify-fabric-api-contract.py
python3 scripts/verify-render-failure-recovery.py
sh -n scripts/build-fabric.sh scripts/preflight.sh
python3 - <<'PYCODE'
from pathlib import Path
for path in (Path('scripts/verify-built-jar.py'), Path('scripts/verify-render-failure-recovery.py'), Path('scripts/verify-fabric-metadata.py'), Path('scripts/verify-fabric-integration.py'), Path('scripts/verify-fabric-api-contract.py')):
    compile(path.read_text(encoding='utf-8'), str(path), 'exec')
print('Python verifier syntax: PASS')
PYCODE
