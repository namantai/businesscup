#!/usr/bin/env bash
set -euo pipefail
COMPASS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$COMPASS_ROOT"
export JAVA_HOME="$COMPASS_ROOT/.tools/jdk-21.0.13+9"
export PATH="$COMPASS_ROOT/.tools/node-v24.22.3-linux-x64/bin:$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME="$COMPASS_ROOT/.cache/maven"
export npm_config_cache="$COMPASS_ROOT/.cache/npm"
export TESTCONTAINERS_RYUK_DISABLED=true
# Ryuk mounts the Docker socket. Tests explicitly stop their own containers instead.
if [[ ! -x "$JAVA_HOME/bin/java" || ! -x "$COMPASS_ROOT/.tools/node-v24.22.3-linux-x64/bin/node" ]]; then
    echo 'Run bash scripts/setup.sh first.' >&2
    exit 1
fi
if [[ ! -f .env ]]; then
    echo 'Missing .env. Run bash scripts/setup.sh.' >&2
    exit 1
fi
while IFS='=' read -r key value || [[ -n "$key" ]]; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    case "$key" in
        FRONTEND_PORT|BACKEND_PORT|DB_PORT|DB_NAME|DB_USER|DB_PASSWORD|LLM_MODE|PAYMENTS_MODE|EMAIL_MODE|SOURCE_FETCH_ENABLED)
            export "$key=$value" ;;
        *) echo "Unsupported .env key: $key" >&2; exit 1 ;;
    esac
done < .env
if [[ -z "${DB_PASSWORD:-}" ]]; then echo 'DB_PASSWORD must be set in .env.' >&2; exit 1; fi
if [[ "${LLM_MODE:-OFF}" != OFF || "${PAYMENTS_MODE:-OFF}" != OFF || "${EMAIL_MODE:-LOCAL}" != LOCAL || "${SOURCE_FETCH_ENABLED:-false}" != false ]]; then
    echo 'P01 only supports disabled external integrations and local email mode.' >&2
    exit 1
fi
for key in FRONTEND_PORT BACKEND_PORT DB_PORT; do
    value="${!key}"
    if [[ ! "$value" =~ ^[0-9]+$ ]] || (( value < 1024 || value > 65535 )); then
        echo "Invalid local port: $key" >&2; exit 1
    fi
done
if [[ "$FRONTEND_PORT" == "$BACKEND_PORT" || "$FRONTEND_PORT" == "$DB_PORT" || "$BACKEND_PORT" == "$DB_PORT" ]]; then
    echo 'Frontend, backend and database ports must be distinct.' >&2
    exit 1
fi
compose() { docker compose --env-file "$COMPASS_ROOT/.env" -f "$COMPASS_ROOT/infra/compose.dev.yml" "$@"; }
maven() { (cd "$COMPASS_ROOT/backend" && bash mvnw -B -ntp -s .mvn/settings.xml "-Dmaven.repo.local=$COMPASS_ROOT/.cache/m2" "$@"); }
