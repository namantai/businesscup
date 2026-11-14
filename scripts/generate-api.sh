#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
# Actual servlet application + isolated PostgreSQL, never the mutable dev server.
maven -Dit.test=ScaffoldIT -Dtest=IntegrationSettingsTest verify
python3 - <<'PY'
import json
with open('docs/openapi.json', 'w') as output:
    json.dump(json.load(open('backend/target/openapi.json')), output, indent=2, ensure_ascii=False, sort_keys=True)
    output.write('\n')
PY
(cd frontend && npm run api:generate)
