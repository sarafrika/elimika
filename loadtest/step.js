// Capped step test: anonymous public reads at a fixed arrival rate per step, stopping at the first step
// whose error rate or p95 breaks its threshold. See loadtest/README.md; run through loadtest/run.sh.
//
// Env: BASE_URL (default http://localhost:38080), STEPS (rps list, default "5,10,20,40,60,80,120,160"),
//      STEP_SECONDS (default 60), COURSE_IDS (comma list of published course UUIDs, required),
//      P95_MS (default 1000), MAX_ERROR_RATE (default 0.01), SEARCH_TERMS (comma list).
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:38080').replace(/\/+$/, '');
const STEPS = (__ENV.STEPS || '5,10,20,40,60,80,120,160')
    .split(',').map((s) => parseInt(s.trim(), 10)).filter((n) => n > 0);
const STEP_SECONDS = parseInt(__ENV.STEP_SECONDS || '60', 10);
const P95_MS = parseInt(__ENV.P95_MS || '1000', 10);
const MAX_ERROR_RATE = parseFloat(__ENV.MAX_ERROR_RATE || '0.01');
const COURSE_IDS = (__ENV.COURSE_IDS || '').split(',').map((s) => s.trim()).filter(Boolean);
const SEARCH_TERMS = (__ENV.SEARCH_TERMS || 'java,python,sql,web,data,design,intro,kotlin')
    .split(',').map((s) => s.trim()).filter(Boolean);

// Weighted like real anonymous traffic: course pages dominate, then search and catalogue.
const MIX = [
    { name: 'course_detail', weight: 30, path: (c) => `/api/v1/courses/${c}` },
    { name: 'course_content', weight: 15, path: (c) => `/api/v1/courses/${c}/content` },
    { name: 'course_reviews', weight: 10, path: (c) => `/api/v1/courses/${c}/reviews` },
    { name: 'course_similar', weight: 10, path: (c) => `/api/v1/courses/${c}/similar` },
    { name: 'commerce_catalogue', weight: 8, path: () => '/api/v1/commerce/catalogue?active_only=true' },
    { name: 'catalogue_search', weight: 14, path: (c, t) => `/api/v1/catalogue/search?q=${encodeURIComponent(t)}&page=0&size=24` },
    { name: 'search_courses', weight: 13, path: (c, t) => `/api/v1/search/courses?q=${encodeURIComponent(t)}` },
];
const TOTAL_WEIGHT = MIX.reduce((a, m) => a + m.weight, 0);

const scenarios = {};
const thresholds = {};
STEPS.forEach((rps, i) => {
    scenarios[`step_${rps}`] = {
        executor: 'constant-arrival-rate',
        rate: rps,
        timeUnit: '1s',
        duration: `${STEP_SECONDS}s`,
        startTime: `${i * STEP_SECONDS}s`,
        preAllocatedVUs: Math.min(Math.max(rps * 2, 5), 400),
        maxVUs: Math.min(Math.max(rps * 6, 20), 1200),
        gracefulStop: '5s',
        tags: { step: String(rps) },
    };
    const delay = `${Math.min(15, Math.max(1, STEP_SECONDS - 1))}s`;
    thresholds[`http_req_failed{step:${rps}}`] = [
        { threshold: `rate<${MAX_ERROR_RATE}`, abortOnFail: true, delayAbortEval: delay },
    ];
    thresholds[`http_req_duration{step:${rps}}`] = [
        { threshold: `p(95)<${P95_MS}`, abortOnFail: true, delayAbortEval: delay },
    ];
    // No-op thresholds so the per-step submetrics exist in handleSummary.
    thresholds[`http_reqs{step:${rps}}`] = ['count>=0'];
    thresholds[`dropped_iterations{scenario:step_${rps}}`] = ['count>=0'];
});

export const options = {
    scenarios,
    thresholds,
    userAgent: 'elimika-loadtest/k6',
    discardResponseBodies: true,
    noConnectionReuse: false,
    summaryTrendStats: ['avg', 'min', 'med', 'p(50)', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
};

export function setup() {
    if (COURSE_IDS.length === 0) {
        throw new Error('COURSE_IDS is empty: pass a comma list of published course UUIDs (run.sh does this)');
    }
    const res = http.get(`${BASE_URL}/api/v1/courses/${COURSE_IDS[0]}`, { tags: { name: 'setup_probe' } });
    if (res.status !== 200) {
        throw new Error(`probe GET /api/v1/courses/${COURSE_IDS[0]} answered ${res.status}; is the app up at ${BASE_URL}?`);
    }
    return { startedAt: Date.now() };
}

function pick() {
    let r = Math.random() * TOTAL_WEIGHT;
    for (const m of MIX) {
        r -= m.weight;
        if (r < 0) return m;
    }
    return MIX[0];
}

export default function () {
    const m = pick();
    const course = COURSE_IDS[Math.floor(Math.random() * COURSE_IDS.length)];
    const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];
    const res = http.get(`${BASE_URL}${m.path(course, term)}`, {
        tags: { name: m.name, endpoint: m.name },
        timeout: '30s',
    });
    check(res, { [`${m.name} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
}

function metricValues(data, key) {
    const m = data.metrics[key];
    return m ? m.values : null;
}

function round(n, digits = 2) {
    return typeof n === 'number' ? Math.round(n * 10 ** digits) / 10 ** digits : null;
}

export function handleSummary(data) {
    const steps = STEPS.map((rps) => {
        const dur = metricValues(data, `http_req_duration{step:${rps}}`);
        const failed = metricValues(data, `http_req_failed{step:${rps}}`);
        const reqs = metricValues(data, `http_reqs{step:${rps}}`);
        const dropped = metricValues(data, `dropped_iterations{scenario:step_${rps}}`);
        const count = reqs ? reqs.count : 0;
        const thr = (key) => {
            const t = data.metrics[key] && data.metrics[key].thresholds;
            return t ? Object.values(t).every((v) => v.ok) : null;
        };
        return {
            step_rps: rps,
            ran: count > 0,
            requests: count,
            achieved_rps: round(count / STEP_SECONDS),
            dropped_iterations: dropped ? dropped.count : 0,
            p50_ms: dur ? round(dur['p(50)']) : null,
            p95_ms: dur ? round(dur['p(95)']) : null,
            p99_ms: dur ? round(dur['p(99)']) : null,
            max_ms: dur ? round(dur.max) : null,
            error_rate: failed ? round(failed.rate, 4) : null,
            passed: count > 0 ? thr(`http_req_failed{step:${rps}}`) !== false && thr(`http_req_duration{step:${rps}}`) !== false : null,
        };
    });
    const ran = steps.filter((s) => s.ran);
    const lastPassing = ran.filter((s) => s.passed).pop();
    const firstFailing = ran.find((s) => s.passed === false);
    const summary = {
        test: 'step',
        base_url: BASE_URL,
        step_seconds: STEP_SECONDS,
        p95_limit_ms: P95_MS,
        max_error_rate: MAX_ERROR_RATE,
        aborted: !!firstFailing,
        max_passing_rps: lastPassing ? lastPassing.step_rps : null,
        first_failing_rps: firstFailing ? firstFailing.step_rps : null,
        steps,
    };
    const json = JSON.stringify(summary, null, 2);
    return {
        stdout: `\n===SUMMARY_JSON_BEGIN===\n${json}\n===SUMMARY_JSON_END===\n`,
    };
}
