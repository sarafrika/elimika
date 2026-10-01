#!/usr/bin/env bash
# Seeds the local stack (idempotent, safe to re-run). Needs the services and the backend running
# (scripts/local/up.sh starts both and calls this). Then rebuilds every search index and refreshes the
# course feature tables, waiting until every index is READY.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
API_URL="${API_URL:-http://localhost:38080}"

command -v python3 >/dev/null || { echo "seed.sh needs python3" >&2; exit 1; }
# HEALTH_URL: override when actuator runs on its own port (scripts/loadtest/stack-up.sh sets it).
curl -sf "${HEALTH_URL:-$API_URL/actuator/health}" >/dev/null || { echo "backend not reachable at $API_URL (run scripts/local/up.sh)" >&2; exit 1; }

exec python3 "$ROOT/scripts/local/seed.py" "$@"
