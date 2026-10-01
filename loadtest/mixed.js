// Signed-in read+write simulation, one scenario per role, against the LOCAL stack only (local Keycloak realm
// elimika-local, qa-* users). Each iteration is one UI "page": the same burst of calls the real page fires,
// sent in parallel with http.batch, including per-item fan-out (e.g. enrollments for each class).
// See loadtest/README.md; run through loadtest/run.sh, which fills the fixture env vars from local postgres.
//
// Env (all optional unless noted):
//   BASE_URL            app, default http://localhost:38080
//   KEYCLOAK_URL        default http://localhost:58080; KEYCLOAK_REALM default elimika-local
//   KEYCLOAK_CLIENT_ID / KEYCLOAK_CLIENT_SECRET  default elimika-ui / elimika-local-ui-secret
//   QA_PASSWORD         default Passw0rd!
//   TARGET_VUS (50), MIXED_SECONDS (300), WRITE_RATIO (0.2: share of page iterations that perform a write),
//   THINK_MIN / THINK_MAX seconds between pages (1 / 3)
//   ROLE_WEIGHTS        default "student:50,instructor:20,course_creator:15,org_admin:15"
//   Fixtures (required, run.sh sets them): STUDENT_UUID, INSTRUCTOR_UUID, CREATOR_UUID, ORG_UUID, COURSE_IDS
//   Optional fixtures: CREATOR_COURSE_IDS, ASSIGNMENT_UUIDS, ENROL_CLASS_UUID, OUTSIDER_STUDENT_UUID
//   ENROL_CYCLE=1       qa-outsider enrols into ENROL_CLASS_UUID then cancels (off by default: paywall/state)
//
// Every write is either idempotent (same skill goals / same location opt-in) or creates an 'LT-' prefixed row
// that the same iteration deletes again; loadtest/cleanup.sql removes anything a crashed run leaves behind.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:38080').replace(/\/+$/, '');
const KEYCLOAK_URL = (__ENV.KEYCLOAK_URL || 'http://localhost:58080').replace(/\/+$/, '');
const REALM = __ENV.KEYCLOAK_REALM || 'elimika-local';
const CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID || 'elimika-ui';
const CLIENT_SECRET = __ENV.KEYCLOAK_CLIENT_SECRET || 'elimika-local-ui-secret';
const PASSWORD = __ENV.QA_PASSWORD || 'Passw0rd!';
const TARGET_VUS = parseInt(__ENV.TARGET_VUS || '50', 10);
const MIXED_SECONDS = parseInt(__ENV.MIXED_SECONDS || '300', 10);
const WRITE_RATIO = parseFloat(__ENV.WRITE_RATIO || '0.2');
const THINK_MIN = parseFloat(__ENV.THINK_MIN || '1');
const THINK_MAX = parseFloat(__ENV.THINK_MAX || '3');
const ENROL_CYCLE = __ENV.ENROL_CYCLE === '1';

const list = (v) => (v || '').split(',').map((s) => s.trim()).filter(Boolean);
const F = {
    student: __ENV.STUDENT_UUID,
    outsider: __ENV.OUTSIDER_STUDENT_UUID,
    instructor: __ENV.INSTRUCTOR_UUID,
    creator: __ENV.CREATOR_UUID,
    org: __ENV.ORG_UUID,
    courses: list(__ENV.COURSE_IDS),
    creatorCourses: list(__ENV.CREATOR_COURSE_IDS),
    assignments: list(__ENV.ASSIGNMENT_UUIDS),
    enrolClass: __ENV.ENROL_CLASS_UUID,
};
const SEARCH_TERMS = list(__ENV.SEARCH_TERMS || 'java,python,sql,web,data,design,intro,kotlin');

const USERS = {
    student: __ENV.USER_STUDENT || 'qa-student@elimika.local',
    outsider: __ENV.USER_OUTSIDER || 'qa-outsider@elimika.local',
    instructor: __ENV.USER_INSTRUCTOR || 'qa-instructor@elimika.local',
    course_creator: __ENV.USER_CREATOR || 'qa-creator@elimika.local',
    org_admin: __ENV.USER_ORGADMIN || 'qa-orgadmin@elimika.local',
};

const PAGES = {
    student: ['browse', 'course_detail', 'my_classes', 'timetable', 'enrol'],
    instructor: ['overview', 'assignments', 'grading'],
    course_creator: ['courses', 'create_course', 'edit_course', 'draft_cycle'],
    org_admin: ['overview', 'student_groups'],
};
const ROLES = Object.keys(PAGES);

function roleWeights() {
    const w = {};
    for (const part of list(__ENV.ROLE_WEIGHTS || 'student:50,instructor:20,course_creator:15,org_admin:15')) {
        const [k, v] = part.split(':');
        if (PAGES[k]) w[k] = parseFloat(v);
    }
    return w;
}

const pageDuration = new Trend('page_duration', true);
const writeOps = new Counter('write_ops');

function buildScenarios() {
    const weights = roleWeights();
    const total = Object.values(weights).reduce((a, b) => a + b, 0) || 1;
    const scenarios = {};
    for (const role of Object.keys(weights)) {
        const vus = Math.max(1, Math.round((TARGET_VUS * weights[role]) / total));
        scenarios[role] = {
            executor: 'ramping-vus',
            exec: role,
            startVUs: 0,
            stages: [
                { duration: `${Math.max(1, Math.round(MIXED_SECONDS * 0.7))}s`, target: vus },
                { duration: `${Math.max(1, Math.round(MIXED_SECONDS * 0.3))}s`, target: vus },
            ],
            gracefulRampDown: '10s',
            gracefulStop: '15s',
            tags: { role },
        };
    }
    return scenarios;
}

function buildThresholds() {
    const t = {};
    for (const role of ROLES) {
        for (const op of ['read', 'write']) {
            // Informational (no abort): they also make the per-role/op submetrics appear in handleSummary.
            t[`http_req_duration{role:${role},op:${op}}`] = ['p(95)<1500'];
            t[`http_req_failed{role:${role},op:${op}}`] = ['rate<0.05'];
            t[`http_reqs{role:${role},op:${op}}`] = ['count>=0'];
        }
        for (const page of PAGES[role]) {
            t[`page_duration{role:${role},page:${page}}`] = ['p(95)>=0'];
        }
    }
    t['http_req_failed{op:auth}'] = ['rate>=0'];
    return t;
}

export const options = {
    scenarios: buildScenarios(),
    thresholds: buildThresholds(),
    userAgent: 'elimika-loadtest/k6',
    summaryTrendStats: ['avg', 'min', 'med', 'p(50)', 'p(90)', 'p(95)', 'p(99)', 'max', 'count'],
    setupTimeout: '60s',
};

// ------------------------------------------------------------------ auth

function fetchToken(username) {
    const res = http.post(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
        grant_type: 'password',
        client_id: CLIENT_ID,
        client_secret: CLIENT_SECRET,
        username,
        password: PASSWORD,
        scope: 'openid',
    }, { tags: { name: 'keycloak_token', op: 'auth' } });
    if (res.status !== 200) {
        throw new Error(`token for ${username}: HTTP ${res.status} ${String(res.body).slice(0, 200)}`);
    }
    const body = res.json();
    return { token: body.access_token, exp: Date.now() + (body.expires_in || 300) * 1000 };
}

let tokenCache = null; // per VU

function token(data, key) {
    if (!tokenCache) tokenCache = Object.assign({}, data.tokens);
    const t = tokenCache[key];
    if (!t || Date.now() > t.exp - 60000) {
        tokenCache[key] = fetchToken(USERS[key]);
    }
    return tokenCache[key].token;
}

// ------------------------------------------------------------------ http helpers

let failuresLogged = 0;

function params(tok, role, page, op, name, extra) {
    return Object.assign({
        headers: { Authorization: `Bearer ${tok}`, 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { role, page, op, name },
        timeout: '30s',
    }, extra || {});
}

function logFailure(res, label) {
    if (res.status >= 400 || res.status === 0) {
        if (failuresLogged < 5) {
            failuresLogged += 1;
            console.warn(`${label}: ${res.request ? res.request.method : ''} ${res.url} -> ${res.status} ${String(res.body || res.error).slice(0, 160)}`);
        }
    }
}

function ok(res, label) {
    logFailure(res, label);
    return check(res, { [`${label} 2xx`]: (r) => r.status >= 200 && r.status < 300 });
}

// Issue a page's burst in parallel. reqs: [name, method, path, body?]
function burst(tok, role, page, op, reqs) {
    const batch = reqs.map(([name, method, path, body]) => ({
        method,
        url: `${BASE_URL}${path}`,
        body: body === undefined ? null : JSON.stringify(body),
        params: params(tok, role, page, op, name),
    }));
    const responses = http.batch(batch);
    responses.forEach((r, i) => ok(r, `${role}.${page}.${reqs[i][0]}`));
    return responses;
}

function req(tok, role, page, op, name, method, path, body, extra) {
    const res = http.request(method, `${BASE_URL}${path}`, body === undefined ? null : JSON.stringify(body),
        params(tok, role, page, op, name, extra));
    ok(res, `${role}.${page}.${name}`);
    if (op === 'write') writeOps.add(1, { role, page });
    return res;
}

function data(res) {
    try {
        const b = res.json();
        return b && b.data !== undefined ? b.data : b;
    } catch (e) {
        return null;
    }
}

function rows(d) {
    if (!d) return [];
    if (Array.isArray(d)) return d;
    if (Array.isArray(d.content)) return d.content;
    return [];
}

const any = (arr) => arr[Math.floor(Math.random() * arr.length)];
const term = () => any(SEARCH_TERMS);
const isoDate = (offsetDays) => new Date(Date.now() + offsetDays * 86400000).toISOString().slice(0, 10);
const marker = () => `LT-${__VU}-${__ITER}-${Date.now()}`;

function timed(role, page, fn) {
    const t0 = Date.now();
    fn();
    pageDuration.add(Date.now() - t0, { role, page });
}

function think() {
    sleep(THINK_MIN + Math.random() * Math.max(0, THINK_MAX - THINK_MIN));
}

const wantsWrite = () => Math.random() < WRITE_RATIO;

// ------------------------------------------------------------------ setup

export function setup() {
    const missing = ['student', 'instructor', 'creator', 'org'].filter((k) => !F[k]);
    if (missing.length || F.courses.length === 0) {
        throw new Error(`missing fixtures ${missing.concat(F.courses.length ? [] : ['COURSE_IDS']).join(', ')}; ` +
            'run through loadtest/run.sh (it reads them from local postgres) after scripts/local/seed.sh');
    }
    const tokens = {};
    const keys = ['student', 'instructor', 'course_creator', 'org_admin'].concat(ENROL_CYCLE ? ['outsider'] : []);
    for (const k of keys) tokens[k] = fetchToken(USERS[k]);

    const auth = (k) => ({ headers: { Authorization: `Bearer ${tokens[k].token}` }, tags: { name: 'setup', op: 'setup' } });

    // Current skill goals, so the student write can PUT the same set back (idempotent).
    const goalsRes = http.get(`${BASE_URL}/api/v1/students/${F.student}/skill-goals`, auth('student'));
    const skillUuids = rows(data(goalsRes)).map((g) => g.skill_uuid).filter(Boolean);

    // Category and difficulty of a creator course, reused for the LT- draft course.
    const courseRes = http.get(`${BASE_URL}/api/v1/courses/${F.creatorCourses[0] || F.courses[0]}`, auth('course_creator'));
    const course = data(courseRes) || {};

    const contentTypes = rows(data(http.get(`${BASE_URL}/api/v1/config/content-types`, auth('course_creator'))));

    return {
        tokens,
        skillUuids,
        categoryUuids: course.category_uuids || [],
        difficultyUuid: course.difficulty_uuid || null,
        contentTypeUuid: contentTypes.length ? contentTypes[0].uuid : null,
    };
}

// ------------------------------------------------------------------ student

export function student(d) {
    const role = 'student';
    const tok = token(d, 'student');
    const s = F.student;
    let page = wantsWrite() ? 'enrol' : any(['browse', 'course_detail', 'course_detail', 'my_classes', 'timetable']);

    timed(role, page, () => {
        if (page === 'browse') {
            burst(tok, role, page, 'read', [
                ['catalogue_search', 'GET', `/api/v1/catalogue/search?q=${encodeURIComponent(term())}&page=0&size=24`],
                ['search_courses', 'GET', `/api/v1/search/courses?q=${encodeURIComponent(term())}`],
                ['commerce_catalogue', 'GET', '/api/v1/commerce/catalogue?active_only=true'],
                ['recommendations', 'GET', '/api/v1/courses/recommendations?limit=12'],
            ]);
        } else if (page === 'course_detail') {
            const c = any(F.courses);
            burst(tok, role, page, 'read', [
                ['course', 'GET', `/api/v1/courses/${c}`],
                ['course_content', 'GET', `/api/v1/courses/${c}/content`],
                ['course_reviews', 'GET', `/api/v1/courses/${c}/reviews`],
                ['course_similar', 'GET', `/api/v1/courses/${c}/similar`],
                ['course_prerequisites', 'GET', `/api/v1/courses/${c}/prerequisites`],
                ['catalogue_resolve', 'GET', `/api/v1/commerce/catalogue/resolve?course_uuid=${c}`],
                ['classes_for_course', 'GET', `/api/v1/classes/course/${c}`],
            ]);
        } else if (page === 'my_classes') {
            const r = burst(tok, role, page, 'read', [
                ['me', 'GET', '/api/v1/users/me'],
                ['student_classes', 'GET', `/api/v1/enrollment/student/${s}/classes?page=0&size=20`],
                ['student_courses', 'GET', `/api/v1/enrollment/student/${s}/courses?page=0&size=20`],
                ['student_overview', 'GET', `/api/v1/enrollment/student/${s}/overview`],
            ]);
            // Fan-out: each class card loads the class and its schedule.
            const classes = rows(data(r[1])).map((x) => x.class_definition_uuid).filter(Boolean).slice(0, 10);
            if (classes.length) {
                burst(tok, role, page, 'read', classes.flatMap((c) => [
                    ['class', 'GET', `/api/v1/classes/${c}`],
                    ['class_schedule', 'GET', `/api/v1/classes/${c}/schedule`],
                ]));
            }
        } else if (page === 'timetable') {
            burst(tok, role, page, 'read', [
                ['student_timetable', 'GET', `/api/v1/timetable/student/${s}?start=${isoDate(-7)}&end=${isoDate(28)}`],
                ['scheduled_instances', 'GET', `/api/v1/enrollment/student/${s}/scheduled-instances?page=0&size=50`],
            ]);
        } else {
            // enrol page: eligibility read, then the write.
            if (F.enrolClass) {
                req(tok, role, page, 'read', 'eligibility', 'GET', `/api/v1/enrollment/eligibility/${F.enrolClass}/student/${s}`);
            }
            if (ENROL_CYCLE && F.enrolClass && F.outsider) {
                const otok = token(d, 'outsider');
                const res = req(otok, role, page, 'write', 'enrol', 'POST', '/api/v1/enrollment',
                    { class_definition_uuid: F.enrolClass, student_uuid: F.outsider });
                for (const e of rows(data(res))) {
                    if (e.uuid) req(otok, role, page, 'write', 'cancel_enrolment', 'DELETE', `/api/v1/enrollment/${e.uuid}`);
                }
            } else {
                req(tok, role, page, 'write', 'skill_goals_put', 'PUT', `/api/v1/students/${s}/skill-goals`,
                    { skill_uuids: d.skillUuids });
            }
        }
    });
    think();
}

// ------------------------------------------------------------------ instructor

export function instructor(d) {
    const role = 'instructor';
    const tok = token(d, 'instructor');
    const i = F.instructor;
    const page = wantsWrite() ? 'grading' : any(['overview', 'overview', 'assignments']);

    timed(role, page, () => {
        if (page === 'overview') {
            const r = burst(tok, role, page, 'read', [
                ['me', 'GET', '/api/v1/users/me'],
                ['instructor', 'GET', `/api/v1/instructors/${i}`],
                ['instructor_classes', 'GET', `/api/v1/classes/instructor/${i}`],
                ['instructor_timetable', 'GET', `/api/v1/timetable/instructor/${i}?start=${isoDate(-7)}&end=${isoDate(28)}`],
                ['pending_grading', 'GET', `/api/v1/assignments/instructor/${i}/pending-grading`],
                ['job_matches', 'GET', '/api/v1/classes/jobs/matches'],
            ]);
            // Fan-out: the overview loads enrollments (and schedule) for every class.
            const classes = rows(data(r[2]))
                .map((x) => (x.class_definition ? x.class_definition.uuid : x.uuid)).filter(Boolean).slice(0, 15);
            if (classes.length) {
                burst(tok, role, page, 'read', classes.flatMap((c) => [
                    ['class_enrollments', 'GET', `/api/v1/classes/${c}/enrollments`],
                    ['class_schedule', 'GET', `/api/v1/classes/${c}/schedule`],
                ]));
            }
        } else if (page === 'assignments') {
            const as = F.assignments.slice(0, 8);
            const reqs = [['pending_grading', 'GET', `/api/v1/assignments/instructor/${i}/pending-grading`]];
            for (const a of as) {
                reqs.push(['assignment', 'GET', `/api/v1/assignments/${a}`]);
                reqs.push(['assignment_submissions', 'GET', `/api/v1/assignments/${a}/submissions`]);
                reqs.push(['assignment_analytics', 'GET', `/api/v1/assignments/${a}/analytics`]);
            }
            burst(tok, role, page, 'read', reqs);
        } else {
            const pending = rows(data(req(tok, role, page, 'read', 'pending_grading', 'GET',
                `/api/v1/assignments/instructor/${i}/pending-grading`)));
            const sub = pending.find((x) => x.uuid && x.assignment_uuid);
            if (sub) {
                const max = sub.max_score || 100;
                const score = Math.round(max * (0.5 + Math.random() * 0.5));
                req(tok, role, page, 'write', 'grade_submission', 'POST',
                    `/api/v1/assignments/${sub.assignment_uuid}/submissions/${sub.uuid}/grade` +
                    `?score=${score}&maxScore=${max}&comments=${encodeURIComponent('LT- load test grade')}`);
            } else {
                // No submissions in the seed: fall back to an idempotent instructor write.
                req(tok, role, page, 'write', 'location_search_put', 'PUT',
                    `/api/v1/instructors/${i}/location-search`, { enabled: true });
            }
        }
    });
    think();
}

// ------------------------------------------------------------------ course creator

export function course_creator(d) {
    const role = 'course_creator';
    const tok = token(d, 'course_creator');
    const cr = F.creator;
    const own = F.creatorCourses.length ? F.creatorCourses : F.courses;
    const page = wantsWrite() ? 'draft_cycle' : any(['courses', 'create_course', 'edit_course']);

    timed(role, page, () => {
        if (page === 'courses') {
            const reqs = [
                ['courses_list', 'GET', '/api/v1/courses?page=0&size=20'],
                ['creator', 'GET', `/api/v1/course-creators/${cr}`],
            ];
            for (const c of own.slice(0, 6)) reqs.push(['course_stats', 'GET', `/api/v1/courses/${c}/stats`]);
            burst(tok, role, page, 'read', reqs);
        } else if (page === 'create_course') {
            burst(tok, role, page, 'read', [
                ['categories', 'GET', '/api/v1/config/categories'],
                ['difficulty_levels', 'GET', '/api/v1/config/difficulty-levels'],
                ['content_types', 'GET', '/api/v1/config/content-types'],
                ['creator', 'GET', `/api/v1/course-creators/${cr}`],
                ['creator_skills', 'GET', `/api/v1/course-creators/${cr}/skills`],
            ]);
        } else if (page === 'edit_course') {
            const c = any(own);
            burst(tok, role, page, 'read', [
                ['course', 'GET', `/api/v1/courses/${c}`],
                ['course_lessons', 'GET', `/api/v1/courses/${c}/lessons?page=0&size=50`],
                ['course_assessments', 'GET', `/api/v1/courses/${c}/assessments`],
                ['course_requirements', 'GET', `/api/v1/courses/${c}/requirements`],
                ['course_skills', 'GET', `/api/v1/courses/${c}/skills`],
                ['course_prerequisites', 'GET', `/api/v1/courses/${c}/prerequisites`],
                ['course_categories', 'GET', `/api/v1/courses/${c}/categories`],
            ]);
        } else {
            draftCycle(d, tok, role, page, cr);
        }
    });
    think();
}

function draftCycle(d, tok, role, page, cr) {
    const name = marker();
    const body = {
        name, course_creator_uuid: cr, category_uuids: d.categoryUuids, difficulty_uuid: d.difficultyUuid,
        description: 'Load test draft course; deleted by the same iteration.', objectives: 'Load test',
        duration_hours: 1, duration_minutes: 0, creator_share_percentage: 60, instructor_share_percentage: 40,
        status: 'draft', active: false, price: 0, minimum_training_fee: 100, class_limit: 30,
    };
    const created = data(req(tok, role, page, 'write', 'course_create', 'POST', '/api/v1/courses', body));
    const uuid = created && created.uuid;
    if (!uuid) return;
    try {
        const lessonBody = {
            course_uuid: uuid, lesson_number: 1, title: `${name} lesson`, status: 'DRAFT', active: false,
            description: 'Load test lesson', learning_objectives: 'Load test',
        };
        const lesson = data(req(tok, role, page, 'write', 'lesson_create', 'POST', `/api/v1/courses/${uuid}/lessons`, lessonBody));
        if (lesson && lesson.uuid) {
            req(tok, role, page, 'write', 'lesson_update', 'PUT', `/api/v1/courses/${uuid}/lessons/${lesson.uuid}`,
                Object.assign({}, lessonBody, { description: 'Load test lesson (edited)' }));
        }
        req(tok, role, page, 'write', 'course_update', 'PUT', `/api/v1/courses/${uuid}`,
            Object.assign({}, body, { description: 'Load test draft course (edited).' }));
        req(tok, role, page, 'read', 'course_reload', 'GET', `/api/v1/courses/${uuid}`);
    } finally {
        req(tok, role, page, 'write', 'course_delete', 'DELETE', `/api/v1/courses/${uuid}`);
    }
}

// ------------------------------------------------------------------ organisation admin

export function org_admin(d) {
    const role = 'org_admin';
    const tok = token(d, 'org_admin');
    const o = F.org;
    const page = wantsWrite() ? 'student_groups' : 'overview';

    timed(role, page, () => {
        if (page === 'overview') {
            burst(tok, role, page, 'read', [
                ['organisation', 'GET', `/api/v1/organisations/${o}`],
                ['org_users', 'GET', `/api/v1/organisations/${o}/users?page=0&size=20`],
                ['org_statistics', 'GET', `/api/v1/organisations/${o}/statistics`],
                ['org_branches', 'GET', `/api/v1/organisations/${o}/training-branches`],
                ['enrolment_trends', 'GET', `/api/v1/enrollment/organisations/${o}/enrolment-trends`],
                ['today_growth', 'GET', `/api/v1/enrollment/organisations/${o}/today-growth`],
                ['weekly_growth', 'GET', `/api/v1/enrollment/organisations/${o}/weekly-growth`],
                ['class_enrolment_counts', 'GET', `/api/v1/enrollment/organisations/${o}/class-enrolment-counts`],
                ['org_activity_feed', 'GET', `/api/v1/enrollment/organisations/${o}/activity-feed`],
                ['student_summaries', 'GET', `/api/v1/enrollment/organisations/${o}/student-summaries`],
                ['org_classes', 'GET', `/api/v1/classes/organisation/${o}`],
                ['instructor_summaries', 'GET', `/api/v1/instructors/organisations/${o}/summaries`],
            ]);
        } else {
            req(tok, role, page, 'read', 'student_groups', 'GET', `/api/v1/organisations/${o}/student-groups`);
            const g = data(req(tok, role, page, 'write', 'student_group_create', 'POST',
                `/api/v1/organisations/${o}/student-groups`, { name: marker(), description: 'Load test group' }));
            if (g && g.uuid) {
                req(tok, role, page, 'write', 'student_group_delete', 'DELETE', `/api/v1/student-groups/${g.uuid}`);
            }
        }
    });
    think();
}

// ------------------------------------------------------------------ summary

const round = (n, digits = 2) => (typeof n === 'number' ? Math.round(n * 10 ** digits) / 10 ** digits : null);

export function handleSummary(summaryData) {
    const m = (k) => (summaryData.metrics[k] ? summaryData.metrics[k].values : null);
    const byRole = {};
    let reads = 0;
    let writes = 0;
    for (const role of ROLES) {
        byRole[role] = { ops: {}, pages: {} };
        for (const op of ['read', 'write']) {
            const dur = m(`http_req_duration{role:${role},op:${op}}`);
            const failed = m(`http_req_failed{role:${role},op:${op}}`);
            const count = (m(`http_reqs{role:${role},op:${op}}`) || {}).count || 0;
            if (op === 'read') reads += count; else writes += count;
            byRole[role].ops[op] = {
                requests: count,
                p50_ms: dur ? round(dur['p(50)']) : null,
                p95_ms: dur ? round(dur['p(95)']) : null,
                p99_ms: dur ? round(dur['p(99)']) : null,
                max_ms: dur ? round(dur.max) : null,
                error_rate: failed && count ? round(failed.rate, 4) : null,
            };
        }
        for (const page of PAGES[role]) {
            const p = m(`page_duration{role:${role},page:${page}}`);
            if (p && p.count) {
                byRole[role].pages[page] = { iterations: p.count, p50_ms: round(p['p(50)']), p95_ms: round(p['p(95)']), p99_ms: round(p['p(99)']) };
            }
        }
    }
    const all = m('http_req_duration') || {};
    const allFailed = m('http_req_failed') || {};
    const summary = {
        test: 'mixed',
        base_url: BASE_URL,
        target_vus: TARGET_VUS,
        mixed_seconds: MIXED_SECONDS,
        write_ratio_pages: WRITE_RATIO,
        write_share_requests: reads + writes ? round(writes / (reads + writes), 4) : null,
        total_requests: (m('http_reqs') || {}).count || 0,
        overall: { p50_ms: round(all['p(50)']), p95_ms: round(all['p(95)']), p99_ms: round(all['p(99)']), error_rate: round(allFailed.rate, 4) },
        auth_error_rate: round((m('http_req_failed{op:auth}') || {}).rate, 4),
        roles: byRole,
    };
    return {
        stdout: `\n===SUMMARY_JSON_BEGIN===\n${JSON.stringify(summary, null, 2)}\n===SUMMARY_JSON_END===\n`,
    };
}
