#!/usr/bin/env python3
from pathlib import Path
source = Path('src/fabric/java/dev/brezze/voxymobile/fabric/VoxyRenderBridge.java').read_text(encoding='utf-8')
checks = {
    'render callback catches runtime faults': 'catch (RuntimeException | LinkageError failure)' in source,
    'failed backend is detached before retry': 'backend = null;' in source and 'nextCreateAttemptNanos = now + CREATE_RETRY_DELAY_NANOS;' in source,
    'backend cleanup is attempted on render thread': 'current.close();' in source,
    'cleanup failure does not escape render callback': 'catch (RuntimeException | LinkageError closeFailure)' in source,
}
for name, passed in checks.items():
    print(('PASS' if passed else 'FAIL') + ': ' + name)
if not all(checks.values()):
    raise SystemExit(1)
print(f'Render failure recovery checks: PASS ({len(checks)})')
