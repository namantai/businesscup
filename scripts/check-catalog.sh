#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
node scripts/check-catalog.mjs
