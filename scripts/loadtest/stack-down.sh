#!/usr/bin/env bash
# Stops the load-test runtime (docker/compose.loadtest.yaml) and the local stack under it. Volumes are KEPT
# (database, search indexes, realm, Prometheus history, Grafana state).
#
#   scripts/loadtest/stack-down.sh                 # stop everything
#   scripts/loadtest/stack-down.sh --keep-local    # stop only the load-test services; postgres/meilisearch/keycloak stay up
#   scripts/loadtest/stack-down.sh --wipe-metrics  # also delete the Prometheus and Grafana volumes
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../local/lib/env.sh
source "$ROOT/scripts/local/lib/env.sh"
COMPOSE=(docker compose -f "$ROOT/docker/compose.local.yaml" -f "$ROOT/docker/compose.loadtest.yaml")

keep_local=0 wipe_metrics=0
for arg in "$@"; do
    case "$arg" in
        --keep-local) keep_local=1 ;;
        --wipe-metrics) wipe_metrics=1 ;;
        *) echo "unknown option: $arg" >&2; exit 2 ;;
    esac
done

LOADTEST_SERVICES=(app postgres-exporter cadvisor prometheus grafana)
if [[ "$keep_local" == 1 ]]; then
    "${COMPOSE[@]}" stop "${LOADTEST_SERVICES[@]}"
    "${COMPOSE[@]}" rm -f "${LOADTEST_SERVICES[@]}"
else
    "${COMPOSE[@]}" --profile app down
fi
if [[ "$wipe_metrics" == 1 ]]; then
    docker volume rm -f elimika-local_elimika_loadtest_prometheus elimika-local_elimika_loadtest_grafana >/dev/null
    echo "[loadtest] removed Prometheus and Grafana volumes"
fi
echo "[loadtest] stopped; data volumes kept"
