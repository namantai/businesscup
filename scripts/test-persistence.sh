#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
bash scripts/smoke.sh
marker="$(python3 -c 'import uuid; print(uuid.uuid4())')"
compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" <<SQL
CREATE TABLE IF NOT EXISTS compass.p01_persistence_probe (marker uuid PRIMARY KEY);
INSERT INTO compass.p01_persistence_probe VALUES ('$marker');
SQL
bash scripts/dev-down.sh
bash scripts/dev-up.sh --no-build
actual="$(compose exec -T postgres psql -At -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -c "SELECT marker FROM compass.p01_persistence_probe WHERE marker = '$marker'")"
[[ "$actual" == "$marker" ]] || { echo 'FAIL: persistence marker mismatch'; exit 1; }
compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -c 'DROP TABLE compass.p01_persistence_probe'
echo 'PASS: persisted marker survived container stop/start; probe table removed.'
