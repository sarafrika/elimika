# Load tests (k6, local stack only)

These scripts drive the **local** stack (`docker/compose.local.yaml` plus the `docker/compose.loadtest.yaml`
overlay). Never point them at staging: `run.sh` refuses any `BASE_URL` containing `staging` or `sarafrika.com`.

```
k6 (docker, --network host) ──HTTP──► API :38080 ──► postgres :55432, meilisearch, keycloak :58080
      │                                  │
      └─ remote write ─► Prometheus :9090 ◄─ scrape actuator :38081 ──► Grafana :3001 (screenshots)
```

## Prerequisites

1. `scripts/local/up.sh --services` then `scripts/local/seed.sh` (seeded courses, `qa-*` users).
2. `scripts/loadtest/stack-up.sh` (API container with staging-like limits, Prometheus, Grafana).

## Run

```
loadtest/run.sh step  <run_name>     # capped step test, anonymous public reads
loadtest/run.sh mixed <run_name>     # signed-in student/instructor/creator/org-admin pages, about 20% writes
```

Quick smoke: `STEPS=2 STEP_SECONDS=10 loadtest/run.sh step smoke` and
`TARGET_VUS=2 MIXED_SECONDS=15 loadtest/run.sh mixed smoke`.

`run.sh` reads the fixtures (published course UUIDs, the qa users' student, instructor and creator UUIDs, the
organisation, assignments) from the local postgres. It runs `grafana/k6` from stdin with Prometheus remote
write (`testid=<run_name>`), when Prometheus is up. It then takes Grafana screenshots and polls
liveness and readiness until both answer 200. Results go to `loadtest/results/<run_name>/` (gitignored):

| File | Content |
|---|---|
| `<test>.log` | full k6 output |
| `<test>-summary.json` | the `SUMMARY_JSON` block: per step (achieved rps, p50/p95/p99, error rate, dropped iterations) or per role/op and page |
| `<test>-window.txt` | start/end epoch ms and the k6 exit code (99 means a threshold was crossed, the expected end of a step test) |
| `<test>/` | Grafana screenshots |
| `<test>-recovery.txt` | how long after the run the app answered healthy again, or `recovered=false` after 180 s |

## Knobs

| Variable | Default | Used by |
|---|---|---|
| `BASE_URL` | `http://localhost:38080` | both |
| `STEPS`, `STEP_SECONDS` | `5,10,20,40,60,80,120,160`, `60` | step |
| `P95_MS`, `MAX_ERROR_RATE` | `1000`, `0.01`: per-step abort thresholds, evaluated after 15 s | step |
| `TARGET_VUS`, `MIXED_SECONDS` | `50`, `300` (ramp over 70 %, hold 30 %) | mixed |
| `WRITE_RATIO` | `0.2`: share of page iterations that write; the summary reports the real write share of requests | mixed |
| `ROLE_WEIGHTS` | `student:50,instructor:20,course_creator:15,org_admin:15` | mixed |
| `THINK_MIN`, `THINK_MAX` | `1`, `3` seconds between pages | mixed |
| `ENROL_CYCLE=1` | off: qa-outsider enrols into a class, then cancels | mixed |
| `KEYCLOAK_URL`, `QA_PASSWORD` | `http://localhost:58080`, `Passw0rd!` | mixed |
| `HEALTH_URL`, `NO_PROM`, `K6_IMAGE` | `:38081` if it answers, else `BASE_URL`; `0`; `grafana/k6:latest` | run.sh |

## What mixed.js writes

Every write either puts back the values it read or creates an `LT-` row that the same iteration deletes:

- student: `PUT /students/{uuid}/skill-goals` with the current set
- instructor: grades a pending submission if one exists, otherwise `PUT /instructors/{uuid}/location-search {enabled:true}`
- course creator: creates an `LT-` draft course, adds and edits a lesson, edits the course, deletes it
- org admin: creates an `LT-` student group, then deletes it

## Cleanup

```
docker compose -f docker/compose.local.yaml exec -T postgres psql -U elimika -d elimika < loadtest/cleanup.sql
```

This deletes `request_audit_log` rows with user agent `elimika-loadtest%`, plus any `LT-` courses and
student groups left behind by interrupted iterations.
