#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
export PLAYWRIGHT_BROWSERS_PATH="$COMPASS_ROOT/.cache/playwright"
# Uses the existing locked browser; installation is a separately authorized setup operation.
python3 scripts/e2e.py "$@"
