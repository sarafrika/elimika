#!/usr/bin/env bash
# Stops the local stack. Volumes are KEPT, so up.sh brings back the same data in seconds.
#
#   scripts/local/down.sh                    # stop backend + containers, keep volumes
#   scripts/local/down.sh --reset-keycloak   # also drop the keycloak database so the realm is re-imported
#   scripts/local/down.sh --wipe             # also delete every volume (database, indexes, realm)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
COMPOSE=(docker compose -f "$ROOT/docker/compose.local.yaml")
RUN_DIR="$ROOT/build/local"

reset_keycloak=0 wipe=0
for arg in "$@"; do
    case "$arg" in
        --reset-keycloak) reset_keycloak=1 ;;
        --wipe) wipe=1 ;;
        *) echo "unknown option: $arg" >&2; exit 2 ;;
    esac
done

if [[ -f "$RUN_DIR/backend.pid" ]]; then
    pid="$(cat "$RUN_DIR/backend.pid")"
    if kill -0 "$pid" 2>/dev/null; then
        echo "[down] stopping bootRun (pid $pid)"
        # Gradle forks the application JVM; stop the whole process group.
        pkill -TERM -P "$pid" 2>/dev/null || true
        kill -TERM "$pid" 2>/dev/null || true
    fi
    rm -f "$RUN_DIR/backend.pid"
fi
# A bootRun started outside up.sh (IDE, terminal) is left alone, but a local-profile JVM still bound to
# :38080 would otherwise keep answering against stopped containers.
if pids=$(pgrep -f 'elimika.*bootRun|ElimikaApplication' 2>/dev/null) && [[ -n "$pids" ]]; then
    echo "[down] note: other Elimika JVMs are still running ($pids); stop them yourself if they belong to this stack"
fi

if [[ "$reset_keycloak" == 1 ]]; then
    echo "[down] dropping the keycloak database (realm is re-imported on next up)"
    "${COMPOSE[@]}" stop keycloak >/dev/null 2>&1 || true
    "${COMPOSE[@]}" exec -T postgres psql -U elimika -d elimika -c 'DROP DATABASE IF EXISTS keycloak WITH (FORCE)' -c 'CREATE DATABASE keycloak OWNER keycloak'
fi

if [[ "$wipe" == 1 ]]; then
    echo "[down] removing containers AND volumes"
    "${COMPOSE[@]}" --profile app down -v
else
    "${COMPOSE[@]}" --profile app down
    echo "[down] stopped; volumes kept (scripts/local/up.sh restarts with the same data)"
fi
