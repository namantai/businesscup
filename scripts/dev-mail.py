#!/usr/bin/env python3
"""Explicit local mailbox viewer. No HTTP listener, SMTP or external transmission."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
MAIL = ROOT / '.runtime/mail'
parser = argparse.ArgumentParser(description='List local development messages, or open one on the loopback frontend.')
parser.add_argument('--open', metavar='MESSAGE_UUID')
args = parser.parse_args()
if not MAIL.exists():
    raise SystemExit('No local messages. Start the app with bash scripts/dev-up.sh.')
if MAIL.is_symlink() or MAIL.stat().st_uid != os.getuid() or MAIL.stat().st_mode & 0o077:
    raise SystemExit('Mailbox must be private (0700) and owned by the current user.')
files = sorted(p for p in MAIL.glob('*.mail') if re.fullmatch(r'[0-9a-f-]{36}\.mail', p.name) and not p.is_symlink())
if not args.open:
    for p in files:
        # Only envelope metadata; never print the token or link into terminal logs.
        lines = p.read_text().splitlines()
        print(p.stem, ' | '.join(lines[:2]))
else:
    if not re.fullmatch(r'[0-9a-f-]{36}', args.open):
        raise SystemExit('Invalid message identifier.')
    p = MAIL / (args.open + '.mail')
    if p not in files or p.stat().st_mode & 0o077:
        raise SystemExit('Message unavailable or permissions are not private.')
    content = p.read_text()
    route = re.search(r'Open in local frontend: (/((verify-email)|(reset-password))#token=[A-Za-z0-9_-]{43})', content)
    if not route:
        raise SystemExit('Invalid local message.')
    port = '5173'
    for line in (ROOT / '.env').read_text().splitlines():
        if line.startswith('FRONTEND_PORT='):
            port = line.split('=', 1)[1]
    if not port.isdigit() or not 1024 <= int(port) <= 65535:
        raise SystemExit('Invalid frontend port.')
    # A desktop handler may echo a failed URL. Keep one-time links out of terminal output.
    try:
        opened = subprocess.run(
            [sys.executable, '-c', 'import sys, webbrowser; sys.exit(0 if webbrowser.open(sys.stdin.read(), new=2) else 1)'],
            input='http://127.0.0.1:' + port + route[1], text=True,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=15,
        ).returncode == 0
    except subprocess.TimeoutExpired:
        opened = False
    if not opened:
        raise SystemExit('No local browser handler. Open the private message in a local editor; do not copy its contents into logs.')
    print('Opened the local verification screen; return to your original tab after verification.')
