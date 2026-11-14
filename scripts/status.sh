#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
compose ps
bash scripts/smoke.sh
compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" <<'SQL'
SELECT version,success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT revision,version_id FROM compass.catalog_active;
SELECT state,count(*) FROM compass.monitor_job GROUP BY state ORDER BY state;
SELECT state,count(*) FROM compass.notification_outbox GROUP BY state ORDER BY state;
SQL
# Show only the configured project listeners; never print environment/configuration secrets.
ss -H -ltn "( sport = :$FRONTEND_PORT or sport = :$BACKEND_PORT or sport = :$DB_PORT )"
