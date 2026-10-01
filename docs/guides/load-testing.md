# Load Testing Guide

This guide covers the local load-test harness: k6 load, Prometheus metrics and a Grafana dashboard that
is captured as screenshots after every run. It also covers what each dashboard row means, the step-test
stop rule, and the procedure for a staging run (run E). Run E is **planned but has not been run**.

The harness runs against the **local** stack only. `loadtest/run.sh` refuses any `BASE_URL` that contains
`staging` or `sarafrika.com`. See `loadtest/README.md` for every knob and `docs/guides/local-environment.md`
for the base local stack.

## Flow

```
LOAD PATH
  k6 (grafana/k6 in docker, host network; step.js or mixed.js)
     │  HTTP :38080 (anonymous reads, or a JWT from Keycloak :58080 for the qa-* users)
     ▼
  API container (Spring Boot; 1536m memory, 2 CPUs)
     │  JDBC through the Hikari pool
     ▼
  Postgres :55432   (the app also calls Meilisearch and Keycloak)

METRICS PATH
  API actuator /actuator/prometheus :38081 ──┐  (HTTP, Hikari, Tomcat, JVM)
  cAdvisor ──────────────────────────────────┤  (container CPU, memory, throttling)
  postgres_exporter ─────────────────────────┼──► Prometheus :9090 ──► Grafana :3001 ──► screenshot.sh
  k6 (remote write, testid=<run_name>) ──────┘    (scrape + store)     (dashboard       (Playwright)
                                                                        elimika-loadtest)     │
                                                                                              ▼
                                                     loadtest/results/<run>/<test>/dashboard.png,
                                                     row-*.png, panel-*.png
```

In short:

- **Load:** k6 sends requests to the app, and the app reads and writes Postgres through the Hikari pool.
- **Metrics:** the app (actuator), cAdvisor and postgres_exporter are scraped by Prometheus. k6 also pushes
  its own client-side metrics to Prometheus. Grafana reads Prometheus, and `scripts/loadtest/screenshot.sh`
  renders the dashboard for the run's time window into PNGs.

## Running the harness

```bash
cd /home/willy/IdeaProjects/elimika
export JAVA_HOME=$HOME/.jdks/temurin-21.0.11 PATH=$HOME/.jdks/temurin-21.0.11/bin:$PATH
./gradlew compileJava

# 1. Base services and fixtures (qa-* users, seeded courses)
scripts/local/up.sh --services
scripts/local/seed.sh

# 2. App container with load-test limits plus Prometheus, Grafana, cAdvisor, postgres_exporter.
#    Stop any bootRun backend on :38080 first (scripts/local/down.sh). Rebuilds the image from the working tree.
scripts/loadtest/stack-up.sh            # --no-build / --no-seed to skip steps

# 3. Smoke the pipeline (10 s per step, 15 s mixed). Check the mixed smoke for 401s.
STEPS=2 STEP_SECONDS=10 loadtest/run.sh step smoke
TARGET_VUS=2 MIXED_SECONDS=15 loadtest/run.sh mixed smoke

# 4. Real runs
loadtest/run.sh step  <run_name>        # anonymous public reads, 5..160 rps, 60 s per step
loadtest/run.sh mixed <run_name>        # signed-in student/instructor/creator/org-admin pages, 50 VUs, 300 s

# 5. Clean up rows written by the run (audit rows, LT- courses and groups)
docker compose -f docker/compose.local.yaml exec -T postgres psql -U elimika -d elimika < loadtest/cleanup.sql

# 6. Stop
scripts/loadtest/stack-down.sh          # --keep-local / --wipe-metrics
```

Results land in `loadtest/results/<run_name>/` (gitignored): `<test>.log`, `<test>-summary.json`,
`<test>-window.txt` (start/end epoch ms and the k6 exit code), `<test>-recovery.txt` and the screenshot folder
`<test>/`. Live dashboard: `http://localhost:3001/d/elimika-loadtest`. Prometheus: `http://localhost:9090`.

Run `cleanup.sql` before and after each run so the audit table does not grow between comparisons.

### Things to know before reading the numbers

- **Recovery time in `<test>-recovery.txt` includes screenshot time.** `run.sh` only starts polling health
  after the screenshots finish, about 95-110 s after k6 ends. If recovery time matters, poll readiness and a
  cheap GET (for example `GET /api/v1/courses/{uuid}` under 500 ms) from a second shell the moment k6 ends.
- **The app image is not a staging copy.** `docker/loadtest/app.Dockerfile` keeps its own ENTRYPOINT with
  `-XX:MaxRAMPercentage=70` (heap max about 1.08 GB). Staging reportedly runs a heap of about 384 MB. The
  container is capped at 2 CPUs, while staging has no CPU limit on a shared 4-core host.
- **k6 runs on the same machine** as the app and Postgres, so they compete for CPU, and there is no network
  latency between them.
- **cAdvisor keeps a stale series** for a replaced app container for a few minutes after `stack-up.sh`
  rebuilds it. Use `max()` rather than `sum()` for container memory and CPU right after a rebuild.
- **The `uri` tag cap.** Micrometer logs "Reached the maximum number of 'uri' tags for
  'http.server.requests'" on long runs. After that, new paths are no longer recorded server side. The
  k6 client-side numbers are not affected.
- **Seed data gap.** Some seeded published courses have no commerce catalogue item, so the mixed test's
  `GET /api/v1/commerce/catalogue/resolve` returns 404 for them. That alone crosses the student-read error
  threshold (k6 exit 99). Read per-endpoint errors before treating a mixed exit 99 as a capacity failure.

## The step-test stop rule

`loadtest/step.js` runs one constant-arrival-rate scenario per step (default
`STEPS=5,10,20,40,60,80,120,160` rps, `STEP_SECONDS=60`), one after another. Each step has two thresholds
with `abortOnFail: true`, evaluated after a 15 s warm-up inside the step (`delayAbortEval`):

| Threshold | Default | Override |
|---|---|---|
| error rate of the step's requests | `< 0.01` (1 %) | `MAX_ERROR_RATE` |
| p95 of the step's requests | `< 1000 ms` | `P95_MS` |

The **first step that crosses either threshold aborts the whole test** (k6 exit 99), and the summary records it
as `first_failing_rps`. The step before it is the maximum sustained rate (`max_passing_rps`, times 60 for
req/min). If every step passes, k6 exits 0 and `first_failing_rps` is `null`. The top step is then a lower
bound, not the app's limit. To find the knee, rerun with higher steps, for example
`STEPS=160,200,250,300,400 loadtest/run.sh step <run_name>`.

Dropped iterations (k6 could not start requests fast enough) are reported per step. Treat a step with
dropped iterations as not held, even if the thresholds passed.

## What each dashboard row means

The dashboard is generated by `docker/observability/grafana/generate-dashboard.py` into
`docker/observability/grafana/dashboards/elimika-loadtest.json`. `screenshot.sh` saves the whole dashboard,
one PNG per row and one PNG per panel.

| Row | Panels | What it tells you |
|---|---|---|
| 1. Load (k6) | requests sent/s by status, `http_req_failed` share, latency p50/p95/p99/max (worst request name), p95 and p99 by request name, virtual users | What the client saw: the offered load, the error share and the latency users would feel, broken down by k6 request name. |
| 2. App (server side) | server p95 and p99 by uri, throughput by status, p50/p95/p99 over all uris, Tomcat threads, process CPU | The same latency measured inside Spring (`http.server.requests`). A large gap between row 1 and row 2 means time is spent queueing before Tomcat, in the accept queue or on the host. Tomcat busy threads near the max means requests are waiting for a thread. |
| 3. DB pool (Hikari) | connections active/idle/pending, connection timeouts/s, acquire time, usage (held) time | The main signal for the staging collapse. Active at the max with pending above 0 means threads are queueing for a connection. Acquire time is how long they wait. Any timeout means the request failed (503 with `Retry-After` after fix 3). A long held time with short queries points at connections held across non-DB work, such as open-in-view or synchronous audit writes. |
| 4. JVM | heap used/committed/max, non-heap, GC pause, live threads | Memory headroom and GC cost. Read the heap max against the local caveat above. A rising floor in heap used (or `jvm_gc_live_data_size_bytes`) over a soak is a leak. |
| 5. Container (cAdvisor) | CPU % (100 = one core), memory working set, CPU throttling (app) | Whole-container use against its limits (2 CPUs = 200 %, 1536 MB). Throttling above 0 means the CPU limit, not the code, is setting latency. |
| 6. Postgres | `pg_stat_activity` by state, transactions/s, locks by mode, cache hit ratio | Whether the database is the bottleneck. Many `active` or `idle in transaction` sessions, lock build-up or a falling cache hit ratio point at the database. Low PG CPU while the pool is saturated points back at the app. |

## Staging run E (planned, not run)

Run E repeats the step test against staging to measure staging's real ceiling under its own heap, CPU and
network. **It has not been run. Running it requires explicit approval from the staging owner first**, for a
named time window. An agent or script must never start it on its own. Until approved, nobody SSHes to the
staging host for this work, and no load is sent to any `*.staging.sarafrika.com` URL.

`loadtest/run.sh` refuses staging URLs on purpose. Run E needs a separate, reviewed wrapper (or a reviewed
one-off override) that is used only for this run and keeps the caps below.

### Procedure

1. **Get approval.** Get written owner approval naming the window, the steps, the abort rule and who is
   watching. Tell anyone using staging that it may become slow or briefly unavailable.
2. **Check what is deployed.** Record the deployed commit and image ID, the container limits, the JVM flags
   (heap), the Hikari and Tomcat settings and `spring.jpa.open-in-view`. Without these the result cannot be
   compared with runs A-D.
3. **Bring up a temporary observability stack** on the staging host: Prometheus, Grafana with the same
   provisioned dashboard, cAdvisor and postgres_exporter. Bind every port to 127.0.0.1 and reach Grafana
   through an SSH tunnel. Scrape the app's `/actuator/prometheus` from inside the docker network or a
   separate management port, never by opening it publicly. Use a read-only Postgres role for the exporter.
4. **Run a baseline check.** Take one idle snapshot of the dashboard and confirm all six rows have data.
5. **Run the capped step test**: `STEPS=5,10,20,40,60`, `STEP_SECONDS=60`, with the same abort rule as
   locally (error rate below 1 %, p95 below 1000 ms, evaluated after 15 s, abort on the first failing step).
   Anonymous public reads only, with no writes and no signed-in mixed traffic, unless the approval says so.
   Run k6 from a machine other than the staging host, so k6 does not take CPU from the app.
6. **Stop by hand on any of these**, even if k6 has not aborted: Hikari pending above 0 for 30 s, any
   connection timeout, any 5xx, a failed container healthcheck, or the owner asking to stop.
7. **Check recovery.** From the moment k6 ends, poll readiness and a cheap GET every second for up to
   180 s. If it does not recover, take a thread dump (`jcmd <pid> Thread.print`) before any restart, then
   restart only with the owner's approval.
8. **Collect results**: the k6 summary, the dashboard screenshots for the window, and the peaks (pool
   active/pending/timeouts, Tomcat busy, CPU, heap, PG CPU). Store them under
   `loadtest/results/E-staging/`, not in git.
9. **Tear down** the temporary observability stack and remove its volumes. Delete the run's
   `request_audit_log` rows (user agent `elimika-loadtest%`) with the owner's approval.
