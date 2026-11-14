#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
import re
import subprocess

names = subprocess.check_output(['git', 'ls-files', '-co', '--exclude-standard', '-z']).decode().split('\0')
patterns = [r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', r'AKIA[0-9A-Z]{16}',
            r'gh[pousr]_[A-Za-z0-9]{30,}', r'sk-(?:proj-)?[A-Za-z0-9_-]{32,}']
local_password = None
if Path('.env').exists():
    for line in Path('.env').read_text().splitlines():
        if line.startswith('DB_PASSWORD='):
            local_password = line.split('=', 1)[1]
bad = []
for name in sorted(set(filter(None, names))):
    path = Path(name)
    if not path.is_file():
        continue
    content = path.read_text(errors='replace')
    if any(re.search(pattern, content) for pattern in patterns) or (local_password and local_password in content):
        bad.append(name)
    if path.name == '.env' or '.runtime' in path.parts or '.tools' in path.parts:
        bad.append(name)
if bad:
    raise SystemExit('Potential secrets or private files (values redacted): ' + ', '.join(sorted(set(bad))))
print('PASS: secret-pattern and local-password scan; no matches. Not a comprehensive secret detector.')
PY
