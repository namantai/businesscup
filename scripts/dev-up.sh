#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
umask 077
compose config --quiet
if [[ "${1:-}" != --no-build ]]; then
    maven package -DskipTests
    (cd frontend && npm ci --no-audit --no-fund)
fi
# Docker refuses an occupied DB port; it never stops the existing listener.
compose up -d --wait --wait-timeout 60 postgres
python3 scripts/local_processes.py start
bash scripts/smoke.sh
printf 'Open http://127.0.0.1:%s\n' "$FRONTEND_PORT"
