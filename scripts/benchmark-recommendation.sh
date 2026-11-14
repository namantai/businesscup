#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
# Explicit benchmark class is excluded from the default *Test suite; no silent skip/pass.
maven -o -Dtest=RecommendationBenchmark test
