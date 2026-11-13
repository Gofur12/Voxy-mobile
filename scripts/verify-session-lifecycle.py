#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
bridge = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyRenderBridge.java').read_text()
client = (root / 'src/fabric/java/dev/brezze/voxymobile/fabric/VoxyMobileClient.java').read_text()
checks = {
    'render bridge exposes session re-enable': 'public static void enable()' in bridge,
    'enable restores render gate': 'enabled = true;' in bridge,
    'enable resets backend initialization state': 'autoCreateAttempted = false;' in bridge and 'nextCreateAttemptNanos = 0L;' in bridge,
    'new connection event registered': 'ClientPlayConnectionEvents.JOIN.register' in client,
    'join event restores the saved render toggle': 'VoxyRenderBridge.setEnabled(VoxyMobileConfig.enabled);' in client,
    'disconnect still disables rendering': 'VoxyRenderBridge.disable();' in client,
    'disconnect still schedules teardown': 'client.execute(() -> {' in client and 'VoxyRenderBridge.detach();' in client,
}
for label, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + label)
if not all(checks.values()):
    raise SystemExit(1)
print(f'Session lifecycle checks: PASS ({len(checks)})')
