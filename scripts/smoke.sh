#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
python3 - <<'PY'
import json
import os
import urllib.request

for port in [os.environ['BACKEND_PORT'], os.environ['FRONTEND_PORT']]:
    url = f'http://127.0.0.1:{port}/actuator/health/readiness'
    with urllib.request.urlopen(url, timeout=5) as response:
        assert response.status == 200
        assert json.load(response) == {'status': 'UP'}
    print('PASS: readiness on port ' + port)
with urllib.request.urlopen(f"http://127.0.0.1:{os.environ['FRONTEND_PORT']}/", timeout=5) as response:
    assert 'AI Compass' in response.read().decode()
print('PASS: frontend HTML; 3 smoke checks passed')
PY
