#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tools/downloads .cache .runtime
python3 - <<'PY'
import hashlib
import os
from pathlib import Path
import secrets
import tarfile
import urllib.request

def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'AI-Compass-setup'}), timeout=120).read()

def install(url, checksum_url, filename, directory):
    destination = Path('.tools') / directory
    if destination.exists():
        return
    checksums = fetch(checksum_url).decode()
    expected = next(line.split()[0] for line in checksums.splitlines() if filename in line or len(checksums.splitlines()) == 1)
    archive = Path('.tools/downloads') / filename
    if not archive.exists() or hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
        archive.write_bytes(fetch(url))
    if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
        raise SystemExit('SHA-256 mismatch: ' + filename)
    with tarfile.open(archive) as tar:
        tar.extractall('.tools', filter='data')
    if not destination.exists():
        raise SystemExit('Unexpected archive layout: ' + filename)
    print('Verified and installed ' + directory)

node = 'node-v24.22.3-linux-x64.tar.xz'
install('https://nodejs.org/dist/v24.22.3/' + node,
        'https://nodejs.org/dist/v24.22.3/SHASUMS256.txt', node, 'node-v24.22.3-linux-x64')
jdk = 'OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz'
base = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/'
install(base + jdk, base + jdk + '.sha256.txt', jdk, 'jdk-21.0.13+9')
if not Path('.env').exists():
    content = Path('.env.example').read_text().replace('DB_PASSWORD=\n', 'DB_PASSWORD=' + secrets.token_hex(32) + '\n')
    fd = os.open('.env', os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as stream:
        stream.write(content)
    print('Created private local .env (password not displayed)')
PY
