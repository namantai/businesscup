#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
if [[ $# != 1 || ! "$1" =~ ^[0-9a-fA-F-]{36}$ ]]; then
    echo 'Usage: bash scripts/admin-bootstrap.sh VERIFIED_ACCOUNT_UUID' >&2
    exit 1
fi
if [[ ! -t 0 ]]; then echo 'Interactive local confirmation is required.' >&2; exit 1; fi
read -r -p 'Grant the first ADMIN role to this verified account? Type GRANT: ' confirmation
[[ "$confirmation" == GRANT ]] || exit 1
java -jar backend/target/ai-compass-0.1.0.jar "--identity-admin=$1" --spring.main.banner-mode=off --logging.level.root=ERROR
