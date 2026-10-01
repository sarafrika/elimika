#!/usr/bin/env bash
# Runs one k6 test (step | mixed) against the LOCAL stack and collects its artefacts.
#
#   loadtest/run.sh step  baseline-01
#   STEPS=2 STEP_SECONDS=10 loadtest/run.sh step smoke
#   TARGET_VUS=2 MIXED_SECONDS=15 loadtest/run.sh mixed smoke
#
# Writes into loadtest/results/<run_name>/:
#   <test>.log              full k6 output
#   <test>-summary.json     the SUMMARY_JSON block from handleSummary
#   <test>-window.txt       start/end epoch ms (the Grafana window) and k6's exit code
#   <test>/                 Grafana screenshots (scripts/loadtest/screenshot.sh), when that script exists
#   <test>-recovery.txt     seconds until liveness + readiness both answer UP again (polled every 2s, 180s max)
#
# Env: BASE_URL (default http://localhost:38080, the API port of docker/compose.loadtest.yaml),
#      HEALTH_URL (default http://localhost:38081 if it answers, else BASE_URL),
#      PROM_RW_URL (default http://localhost:9090/api/v1/write; skipped with a warning when Prometheus is down,
#      or set NO_PROM=1), K6_IMAGE (default grafana/k6:latest), plus every env var step.js/mixed.js read.
# Local only: never point BASE_URL at staging.
set -uo pipefail

usage() { echo "usage: $0 <step|mixed> <run_name>" >&2; exit 2; }
[[ $# -eq 2 ]] || usage
TEST="$1"
RUN="$2"
[[ "$TEST" == step || "$TEST" == mixed ]] || usage
[[ "$RUN" =~ ^[A-Za-z0-9._-]+$ ]] || { echo "run_name must match [A-Za-z0-9._-]+" >&2; exit 2; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT/loadtest/$TEST.js"
OUT="$ROOT/loadtest/results/$RUN"
mkdir -p "$OUT"

export BASE_URL="${BASE_URL:-http://localhost:38080}"
case "$BASE_URL" in
    *staging*|*sarafrika.com*) echo "refusing to load-test $BASE_URL: local stack only" >&2; exit 2 ;;
esac
if [[ -z "${HEALTH_URL:-}" ]]; then
    if curl -s -m 2 -o /dev/null "http://localhost:38081/actuator/health/liveness"; then
        HEALTH_URL="http://localhost:38081"
    else
        HEALTH_URL="$BASE_URL"
    fi
fi
PROM_RW_URL="${PROM_RW_URL:-http://localhost:9090/api/v1/write}"
K6_IMAGE="${K6_IMAGE:-grafana/k6:latest}"
COMPOSE=(docker compose -f "$ROOT/docker/compose.local.yaml")

psql_q() {
    "${COMPOSE[@]}" exec -T postgres psql -U elimika -d elimika -AtX -c "$1" 2>/dev/null | paste -sd, -
}

# ------------------------------------------------------------------ fixtures (env wins over the database)
echo "== fixtures from local postgres"
: "${COURSE_IDS:=$(psql_q "SELECT uuid FROM courses WHERE status = 'published' AND parent_course_uuid IS NULL
    AND name NOT LIKE 'LT-%' ORDER BY id LIMIT 50")}"
user_role_uuid() { psql_q "SELECT t.uuid FROM $1 t JOIN users u ON u.uuid = t.user_uuid WHERE u.email = '$2' LIMIT 1"; }
: "${STUDENT_UUID:=$(user_role_uuid students qa-student@elimika.local)}"
: "${OUTSIDER_STUDENT_UUID:=$(user_role_uuid students qa-outsider@elimika.local)}"
: "${INSTRUCTOR_UUID:=$(user_role_uuid instructors qa-instructor@elimika.local)}"
: "${CREATOR_UUID:=$(user_role_uuid course_creators qa-creator@elimika.local)}"
: "${ORG_UUID:=$(psql_q "SELECT uuid FROM organisation WHERE name = 'Nairobi Code Academy' LIMIT 1")}"
: "${CREATOR_COURSE_IDS:=$(psql_q "SELECT uuid FROM courses WHERE course_creator_uuid = '${CREATOR_UUID:-00000000-0000-0000-0000-000000000000}'
    AND parent_course_uuid IS NULL AND name NOT LIKE 'LT-%' ORDER BY id LIMIT 20")}"
: "${ASSIGNMENT_UUIDS:=$(psql_q "SELECT DISTINCT a.uuid FROM assignments a JOIN lessons l ON l.uuid = a.lesson_uuid
    JOIN class_definitions cd ON cd.course_uuid = l.course_uuid
    WHERE cd.default_instructor_uuid = '${INSTRUCTOR_UUID:-00000000-0000-0000-0000-000000000000}' LIMIT 20")}"
: "${ENROL_CLASS_UUID:=$(psql_q "SELECT uuid FROM class_definitions WHERE organisation_uuid = '${ORG_UUID:-00000000-0000-0000-0000-000000000000}'
    ORDER BY id LIMIT 1")}"
export COURSE_IDS STUDENT_UUID OUTSIDER_STUDENT_UUID INSTRUCTOR_UUID CREATOR_UUID ORG_UUID CREATOR_COURSE_IDS \
    ASSIGNMENT_UUIDS ENROL_CLASS_UUID
if [[ -z "$COURSE_IDS" ]]; then
    echo "no published courses found (is docker/compose.local.yaml up and seeded? scripts/local/seed.sh)" >&2
    exit 1
fi
echo "   courses=$(tr ',' '\n' <<<"$COURSE_IDS" | wc -l) student=${STUDENT_UUID:-?} instructor=${INSTRUCTOR_UUID:-?}" \
     "creator=${CREATOR_UUID:-?} org=${ORG_UUID:-?} assignments=$(tr ',' '\n' <<<"$ASSIGNMENT_UUIDS" | grep -c . || true)"

# ------------------------------------------------------------------ k6
PASS_VARS=(BASE_URL COURSE_IDS STEPS STEP_SECONDS P95_MS MAX_ERROR_RATE SEARCH_TERMS
    KEYCLOAK_URL KEYCLOAK_REALM KEYCLOAK_CLIENT_ID KEYCLOAK_CLIENT_SECRET QA_PASSWORD
    TARGET_VUS MIXED_SECONDS WRITE_RATIO THINK_MIN THINK_MAX ROLE_WEIGHTS ENROL_CYCLE
    STUDENT_UUID OUTSIDER_STUDENT_UUID INSTRUCTOR_UUID CREATOR_UUID ORG_UUID CREATOR_COURSE_IDS
    ASSIGNMENT_UUIDS ENROL_CLASS_UUID USER_STUDENT USER_OUTSIDER USER_INSTRUCTOR USER_CREATOR USER_ORGADMIN)
ENV_ARGS=()
for v in "${PASS_VARS[@]}"; do
    [[ -n "${!v:-}" ]] && ENV_ARGS+=(-e "$v=${!v}")
done

OUTPUT_ARGS=()
if [[ "${NO_PROM:-0}" != 1 ]] && curl -s -m 2 -o /dev/null "${PROM_RW_URL%/api/v1/write}/-/ready"; then
    OUTPUT_ARGS=(-o experimental-prometheus-rw)
    ENV_ARGS+=(-e "K6_PROMETHEUS_RW_SERVER_URL=$PROM_RW_URL"
        -e "K6_PROMETHEUS_RW_TREND_STATS=p(50),p(95),p(99),max"
        -e "K6_PROMETHEUS_RW_PUSH_INTERVAL=5s")
else
    echo "   (Prometheus remote-write not reachable or NO_PROM=1: metrics stay in the k6 log only)"
fi

LOG="$OUT/$TEST.log"
echo "== k6 $TEST, run $RUN -> $LOG"
START_MS=$(date +%s%3N)
docker run --rm -i --network host "${ENV_ARGS[@]}" "$K6_IMAGE" \
    run "${OUTPUT_ARGS[@]}" --tag "testid=$RUN" --tag "test=$TEST" - <"$SCRIPT" 2>&1 | tee "$LOG"
K6_EXIT=${PIPESTATUS[0]}
END_MS=$(date +%s%3N)
printf 'start_ms=%s\nend_ms=%s\nk6_exit=%s\n' "$START_MS" "$END_MS" "$K6_EXIT" >"$OUT/$TEST-window.txt"
# k6 exits 99 when a threshold fails (expected when the step test finds the knee).
echo "== k6 exit $K6_EXIT (99 = threshold crossed), window $START_MS..$END_MS"

sed -n '/===SUMMARY_JSON_BEGIN===/,/===SUMMARY_JSON_END===/p' "$LOG" | sed '1d;$d' >"$OUT/$TEST-summary.json"
if [[ -s "$OUT/$TEST-summary.json" ]] && command -v jq >/dev/null; then
    jq -c 'if .test == "step" then {max_passing_rps, first_failing_rps}
           else {total_requests, overall, write_share_requests} end' "$OUT/$TEST-summary.json" || true
fi

# ------------------------------------------------------------------ screenshots
SHOT="$ROOT/scripts/loadtest/screenshot.sh"
if [[ -x "$SHOT" ]]; then
    echo "== screenshots -> $OUT/$TEST/"
    # Pad the window a little so the edges of the run are visible.
    "$SHOT" "$((START_MS - 30000))" "$((END_MS + 60000))" "$OUT/$TEST/" || echo "   screenshot.sh failed (continuing)"
fi

# ------------------------------------------------------------------ recovery
echo "== waiting for liveness + readiness at $HEALTH_URL (2s poll, 180s max)"
REC="$OUT/$TEST-recovery.txt"
t0=$(date +%s%3N)
recovered=""
for _ in $(seq 1 90); do
    live=$(curl -s -m 2 -o /dev/null -w '%{http_code}' "$HEALTH_URL/actuator/health/liveness")
    ready=$(curl -s -m 2 -o /dev/null -w '%{http_code}' "$HEALTH_URL/actuator/health/readiness")
    if [[ "$live" == 200 && "$ready" == 200 ]]; then
        recovered=$(( $(date +%s%3N) - t0 ))
        break
    fi
    sleep 2
done
{
    echo "health_url=$HEALTH_URL"
    echo "k6_end_ms=$END_MS"
    echo "poll_start_ms=$t0"
    if [[ -n "$recovered" ]]; then
        echo "recovered=true"
        echo "recovery_ms_after_poll_start=$recovered"
        echo "recovery_ms_after_k6_end=$(( t0 + recovered - END_MS ))"
    else
        echo "recovered=false"
        echo "last_liveness_status=${live:-none}"
        echo "last_readiness_status=${ready:-none}"
    fi
} | tee "$REC"

exit 0
