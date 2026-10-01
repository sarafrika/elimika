#!/usr/bin/env bash
# One command to a seeded local Elimika: services (docker/compose.local.yaml), the API via bootRun with the
# `local` profile, then scripts/local/seed.sh. Safe to re-run: a backend already answering on :38080 is reused.
#
#   scripts/local/up.sh               # services + backend + seed
#   scripts/local/up.sh --no-seed     # services + backend
#   scripts/local/up.sh --services    # services only (run the API yourself, e.g. from the IDE)
#   scripts/local/up.sh --container   # backend as a container (compose profile "app") instead of bootRun
#
# Backend log: build/local/backend.log; PID: build/local/backend.pid. Stop everything with down.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
COMPOSE=(docker compose -f "$ROOT/docker/compose.local.yaml")
API_URL="${API_URL:-http://localhost:38080}"
RUN_DIR="$ROOT/build/local"
mkdir -p "$RUN_DIR"

seed=1 backend=bootrun
for arg in "$@"; do
    case "$arg" in
        --no-seed) seed=0 ;;
        --services) backend=none; seed=0 ;;
        --container) backend=container ;;
        *) echo "unknown option: $arg" >&2; exit 2 ;;
    esac
done

log() { printf '\033[1;34m[up]\033[0m %s\n' "$*"; }

log "starting postgres, meilisearch and keycloak"
"${COMPOSE[@]}" up -d --wait postgres meilisearch keycloak

api_up() { curl -sf "$API_URL/actuator/health" >/dev/null 2>&1; }

if [[ "$backend" == container ]]; then
    log "building and starting the backend container"
    "${COMPOSE[@]}" --profile app up -d --build backend
elif [[ "$backend" == bootrun ]]; then
    if api_up; then
        log "backend already answering on $API_URL; reusing it"
    else
        if [[ -z "${JAVA_HOME:-}" && -d "$HOME/.jdks/temurin-21.0.11" ]]; then
            export JAVA_HOME="$HOME/.jdks/temurin-21.0.11"
        fi
        log "starting the backend (bootRun, profile local); log: $RUN_DIR/backend.log"
        (
            cd "$ROOT"
            # UTC is the source of truth (AGENTS.md); the container image pins it the same way.
            SPRING_PROFILES_ACTIVE=local JAVA_TOOL_OPTIONS="-Duser.timezone=UTC ${JAVA_TOOL_OPTIONS:-}" \
                nohup ./gradlew bootRun --console=plain >"$RUN_DIR/backend.log" 2>&1 &
            echo $! >"$RUN_DIR/backend.pid"
        )
    fi
fi

if [[ "$backend" != none ]]; then
    log "waiting for $API_URL/actuator/health"
    for _ in $(seq 1 180); do
        api_up && break
        if [[ -f "$RUN_DIR/backend.pid" ]] && ! kill -0 "$(cat "$RUN_DIR/backend.pid")" 2>/dev/null; then
            echo "backend exited; see $RUN_DIR/backend.log" >&2
            exit 1
        fi
        sleep 2
    done
    api_up || { echo "backend did not become healthy; see $RUN_DIR/backend.log" >&2; exit 1; }
    log "backend is up"
fi

if [[ "$seed" == 1 ]]; then
    "$ROOT/scripts/local/seed.sh"
fi

log "ready: API $API_URL  Keycloak http://localhost:58080 (admin/admin)  Meilisearch http://localhost:57700"
