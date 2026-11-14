#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
python3 - <<'PY'
import json
assert json.load(open('backend/target/openapi.json')) == json.load(open('docs/openapi.json')), 'OpenAPI drift: run bash scripts/generate-api.sh and review the change'
PY
COMPASS_CLIENT_CHECK_DIR="$(mktemp -d "$COMPASS_ROOT/.runtime/client-check-XXXXXXXX")"
export COMPASS_CLIENT_OUTPUT="$COMPASS_CLIENT_CHECK_DIR"
(cd frontend && npm run api:generate)
diff -r frontend/src/api/generated "$COMPASS_CLIENT_CHECK_DIR"
echo 'PASS: live backend OpenAPI and reproducible generated client match repository artifacts.'
