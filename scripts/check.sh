#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
docker info --format '{{.ServerVersion}}' >/dev/null
compose config --quiet
bash scripts/check-secrets.sh
maven clean verify
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm run typecheck
npm test
npm run build
cd "$COMPASS_ROOT"
bash scripts/check-catalog.sh
