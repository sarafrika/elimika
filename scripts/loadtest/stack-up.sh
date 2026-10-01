#!/usr/bin/env bash
# Brings up the load-test runtime: the local stack (postgres, meilisearch, keycloak from docker/compose.local.yaml)
# plus docker/compose.loadtest.yaml (the API built from the CURRENT working tree as a container with staging-like
# limits, Prometheus, Grafana, postgres_exporter, cAdvisor). Local only; nothing talks to staging.
#
#   scripts/loadtest/stack-up.sh              # build the app image, start everything, seed
#   scripts/loadtest/stack-up.sh --no-build   # reuse the last built image
#   scripts/loadtest/stack-up.sh --no-seed    # skip scripts/local/seed.sh
#   LOADTEST_APP_DOCKERFILE=Dockerfile scripts/loadtest/stack-up.sh   # build ./Dockerfile verbatim (slow)
#
# A bootRun backend on :38080 (scripts/local/up.sh) must be stopped first: the container publishes the same port.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../local/lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
COMPOSE=(docker compose -f "$ROOT/docker/compose.local.yaml" -f "$ROOT/docker/compose.loadtest.yaml")
API_URL="${API_URL:-http://localhost:38080}"

build=1 seed=1
for arg in "$@"; do
    case "$arg" in
        --no-build) build=0 ;;
        --no-seed) seed=0 ;;
        *) echo "unknown option: $arg" >&2; exit 2 ;;
    esac
done
log() { printf '\033[1;35m[loadtest]\033[0m %s\n' "$*"; }

# Refuse to start next to a backend that is not our container (bootRun, IDE): both want :38080.
if curl -sf "$API_URL/actuator/health" >/dev/null 2>&1 \
    && [[ -z "$("${COMPOSE[@]}" ps -q --status running app 2>/dev/null)" ]]; then
    echo "something already answers on $API_URL (bootRun?). Stop it first: scripts/local/down.sh" >&2
    exit 1
fi
# The plain compose.local.yaml backend container (profile "app") also binds :38080.
"${COMPOSE[@]}" --profile app rm -sf backend >/dev/null 2>&1 || true

log "starting postgres, meilisearch and keycloak"
"${COMPOSE[@]}" up -d --wait postgres meilisearch keycloak

if [[ "$build" == 1 ]]; then
    if [[ "${LOADTEST_APP_DOCKERFILE:-docker/loadtest/app.Dockerfile}" == docker/loadtest/app.Dockerfile ]]; then
        if [[ -z "${JAVA_HOME:-}" && -d "$HOME/.jdks/temurin-21.0.11" ]]; then
            export JAVA_HOME="$HOME/.jdks/temurin-21.0.11" PATH="$HOME/.jdks/temurin-21.0.11/bin:$PATH"
        fi
        log "building the jar from the working tree (./gradlew bootJar)"
        rm -f "$ROOT"/build/libs/*.jar
        (cd "$ROOT" && ./gradlew bootJar --console=plain -q)
        jar="$(cd "$ROOT" && ls build/libs/*.jar | grep -v -- '-plain.jar' | head -1)"
        log "building the app image around $jar (docker/loadtest/app.Dockerfile)"
        "${COMPOSE[@]}" build --build-arg "JAR=$jar" app
    else
        log "building the app image with $LOADTEST_APP_DOCKERFILE (slow on a cold Gradle cache)"
        "${COMPOSE[@]}" build app
    fi
fi

log "starting app, prometheus, grafana, postgres-exporter, cadvisor"
"${COMPOSE[@]}" up -d --wait app postgres-exporter cadvisor prometheus grafana

if [[ "$seed" == 1 ]]; then
    log "seeding (idempotent)"
    API_URL="$API_URL" HEALTH_URL=http://localhost:38081/actuator/health "$ROOT/scripts/local/seed.sh"
fi

cat <<INFO
$(log "ready")
  API                 $API_URL           (container, mem 1536m, 2 cpus)
  Actuator / metrics  http://localhost:38081/actuator/prometheus
  Prometheus          http://localhost:9090   (k6 remote write: http://localhost:9090/api/v1/write)
  Grafana             http://localhost:3001/d/elimika-loadtest   (anonymous, no login)
  cAdvisor            http://localhost:58088
  postgres_exporter   http://localhost:59187/metrics
  k6 env:             K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \\
                      K6_PROMETHEUS_RW_TREND_STATS='p(50),p(95),p(99),max' k6 run -o experimental-prometheus-rw ...
  k6 in docker:       docker run --rm -i --network elimika-local_default \\
                        -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \\
                        -e K6_PROMETHEUS_RW_TREND_STATS='p(50),p(95),p(99),max' \\
                        grafana/k6 run -o experimental-prometheus-rw --tag testid=<run> - < script.js   (API: http://app:38080)
  screenshots:        scripts/loadtest/screenshot.sh <from_ms> <to_ms> loadtest/results/<run>
INFO
