#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
bash scripts/smoke.sh
export PLAYWRIGHT_BROWSERS_PATH="$COMPASS_ROOT/.cache/playwright"
cd frontend
# Browser revision is fixed by the locked Playwright package. No install-deps/sudo.
./node_modules/.bin/playwright install chromium
npm run test:e2e
