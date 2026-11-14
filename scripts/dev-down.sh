#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
python3 scripts/local_processes.py stop
compose stop
echo 'AI Compass stopped. Database volume retained.'
