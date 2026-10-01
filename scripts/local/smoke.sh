#!/usr/bin/env bash
# Runs the local rule-matrix smoke suite (scripts/local/smoke.py) against the seeded stack.
# Exit code is non-zero when any check fails. See docs/guides/local-environment.md.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
API_URL="${API_URL:-http://localhost:38080}"
# HEALTH_URL: override when actuator runs on its own port (scripts/loadtest/stack-up.sh sets it).
curl -sf "${HEALTH_URL:-$API_URL/actuator/health}" >/dev/null || { echo "backend not reachable at $API_URL (run scripts/local/up.sh)" >&2; exit 1; }
exec python3 "$ROOT/scripts/local/smoke.py" "$@"
