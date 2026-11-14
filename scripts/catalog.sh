#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
if (( $# < 1 )); then
    echo 'Usage: bash scripts/catalog.sh validate|import|review|publish|rollback|current [--file=... --version=... --hash=... --actor=... --expected-revision=... --reason=...]' >&2
    exit 2
fi
command="$1"
shift
# Existing local database only; no automatic import, review or activation.
exec java -jar backend/target/ai-compass-0.1.0.jar "--catalog-command=$command" --spring.main.banner-mode=off --logging.level.root=ERROR "$@"
