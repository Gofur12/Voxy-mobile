#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"
sh scripts/verify-core.sh
python3 scripts/verify-session-lifecycle.py
python3 scripts/verify-build-bootstrap.py
