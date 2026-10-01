#!/usr/bin/env python3
"""End-to-end rule matrix for search, permissions and recommendations against the seeded local stack.

Logs in as each QA role (password grant on the local realm) and asserts what each may and may not see.
Prints PASS/FAIL per check, a summary, and exits non-zero on any failure. PENDING marks a check for a feature
that is documented as not wired yet on this base; it is reported but does not fail the run.

Some checks change state and put it back: an instructor's location opt-in, a membership row, and Meilisearch
itself (stopped and restarted for the "search down" checks). Run scripts/local/seed.sh first.
"""
import json
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "lib"))
from elimika_local import COMPOSE, call, q, sql, sql_value  # noqa: E402

RESULTS = []
NEAR = "-1.2921,36.8219"


def check(name, ok, detail=""):
    RESULTS.append(("PASS" if ok else "FAIL", name, detail))
    tag = "\033[32mPASS\033[0m" if ok else "\033[31mFAIL\033[0m"
    print(f"  {tag} {name}" + ("" if ok or not detail else f"  -- {detail}"), flush=True)
    return ok


def pending(name, detail):
    RESULTS.append(("PENDING", name, detail))
    print(f"  \033[33mPENDING\033[0m {name}  -- {detail}", flush=True)


def section(title):
    print(f"\n== {title}", flush=True)


def course(name):
    return sql_value(f"SELECT uuid FROM courses WHERE name = {q(name)} AND parent_course_uuid IS NULL")


def gsearch(user, text, types=None, **extra):
    params = {"q": text, "limit": 20}
    if types:
        params["types"] = types
    params.update(extra)
    return call("GET", "/api/v1/search", user, params=params)


def totals(resp):
    return (resp.data or {}).get("totals", {}) if resp.status == 200 else {}


def hits(resp, type_=None):
    data = resp.data or {}
    rows = data.get("hits") or data.get("content") or []
    return [h for h in rows if type_ is None or h.get("type") == type_]


def page(resp):
    data = resp.data
    if isinstance(data, dict) and "content" in data:
        return data["content"]
    return data if isinstance(data, list) else []


def names(rows, key="name"):
    return [r.get(key) or r.get("title") for r in rows]


def raw_coordinates(text):
    """Numbers (outside JSON strings) with more than 2 decimals - what a raw coordinate looks like."""
    without_strings = re.sub(r'"(?:[^"\\]|\\.)*"', '""', text)
    return re.findall(r"-?\d{1,3}\.\d{3,}", without_strings)


def recommended(user, **params):
    resp = call("GET", "/api/v1/courses/recommendations", user, params={"limit": 20, **params})
    return resp, (resp.data if resp.status == 200 and isinstance(resp.data, list) else [])


def wait_for(predicate, timeout=60, interval=2):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if predicate():
            return True
        time.sleep(interval)
    return False


def compose(*args):
    subprocess.run(COMPOSE + list(args), check=True, capture_output=True)


def main():
    F = {
        "js": course("JavaScript Fundamentals"), "js_adv": course("Advanced JavaScript Patterns"),
        "python": course("Python for Data Analysis"), "sql": course("SQL Essentials"),
        "web": course("Web Accessibility Basics"), "trading": course("Algorithmic Trading with Python"),
        "draft": course("Rust Systems Draft"), "archived": course("Legacy jQuery Widgets"),
        "shadow": sql_value("SELECT uuid FROM courses WHERE parent_course_uuid IS NOT NULL LIMIT 1"),
        "org": sql_value("SELECT uuid FROM organisation WHERE name = 'Nairobi Code Academy'"),
        "org2": sql_value("SELECT uuid FROM organisation WHERE name = 'Mombasa Data School'"),
        "job_open": sql_value("SELECT uuid FROM class_marketplace_jobs WHERE title = 'JavaScript Weekend Trainer'"),
        "minor": sql_value("SELECT s.uuid FROM students s JOIN users u ON u.uuid = s.user_uuid "
                           "WHERE u.email = 'qa-minor@elimika.local'"),
        "student": sql_value("SELECT s.uuid FROM students s JOIN users u ON u.uuid = s.user_uuid "
                             "WHERE u.email = 'qa-student@elimika.local'"),
        "student_user": sql_value("SELECT uuid FROM users WHERE email = 'qa-student@elimika.local'"),
        "inst": sql_value("SELECT i.uuid FROM instructors i JOIN users u ON u.uuid = i.user_uuid "
                          "WHERE u.email = 'qa-instructor@elimika.local'"),
    }
    missing = [k for k, v in F.items() if not v]
    if missing:
        sys.exit(f"seed data missing ({missing}); run scripts/local/seed.sh first")

    # ------------------------------------------------------------------ anonymous
    section("Anonymous")
    r = gsearch(None, "javascript")
    allowed = {"courses", "programs", "classes", "organisations"}
    check("global search answers anonymously", r.status == 200, r)
    check("global search shows no people/instructors/jobs/rubrics/course_content",
          set(totals(r)) <= allowed and not {h["type"] for h in hits(r)} - allowed, totals(r))
    r = gsearch(None, "rust")
    check("a draft course is not searchable", F["draft"] not in [h["uuid"] for h in hits(r)], hits(r))
    r = gsearch(None, "ARIA live regions")
    check("a shadow-draft edit is not searchable",
          F["shadow"] not in [h["uuid"] for h in hits(r)] and not any("PENDING" in (h.get("highlight") or "")
                                                                      for h in hits(r)), hits(r))
    r = gsearch(None, "javscript", "courses")
    check("typo 'javscript' finds JavaScript Fundamentals", F["js"] in [h["uuid"] for h in hits(r)], hits(r))
    r = gsearch(None, "pyton", "courses")
    check("typo 'pyton' finds Python for Data Analysis", F["python"] in [h["uuid"] for h in hits(r)], hits(r))
    r = call("GET", f"/api/v1/courses/{F['js']}")
    check("GET /courses/{uuid} is authenticated on this base (401, not permitAll)", r.status == 401, r)
    r = call("GET", f"/api/v1/courses/{F['js']}/content")
    check("public course summary (/content) answers anonymously", r.status == 200
          and r.data.get("access") == "prospect", r)
    r = call("GET", f"/api/v1/courses/{F['draft']}/content")
    check("draft course summary (/content) is 404 anonymously", r.status == 404, r)
    r = call("GET", f"/api/v1/courses/{F['shadow']}/content")
    check("shadow-draft summary (/content) is 404 anonymously", r.status == 404, r)
    r = call("GET", f"/api/v1/courses/{F['js']}/similar")
    check("/similar works anonymously and explains itself", r.status == 200 and r.data
          and all(i.get("reasons") for i in r.data), r)
    r = call("GET", f"/api/v1/courses/{F['draft']}/similar")
    check("/similar of a draft is 404", r.status == 404, r)
    r = call("GET", "/api/v1/search/courses", params={"name_like": "java"})
    check("_like on the per-type search API is 400", r.status == 400, r)
    r = call("GET", "/api/v1/courses/search", "qa-student", params={"name_like": "java"})
    check("_like on /courses/search is 400", r.status == 400, r)
    r = call("GET", "/api/v1/users/search", params={"q": "qa"})
    check("/users/search is 401 anonymously", r.status == 401, r)
    r = call("GET", "/api/v1/search/instructors", params={"near": NEAR})
    check("near-me needs a sign-in (403 on global search)", r.status == 403, r)

    # ------------------------------------------------------------------ student / outsider
    for user in ("qa-student", "qa-outsider"):
        section(f"Student: {user}")
        r = gsearch(user, "sam", "people")
        check("people search yields nothing", r.status in (200, 403) and not hits(r, "people"), r)
        r = gsearch(user, "sam")
        check("people type hidden from global search", "people" not in totals(r), totals(r))
        r = gsearch(user, "lesson", "course_content")
        content = hits(r, "course_content")
        if user == "qa-student":
            check("course_content shows the enrolled course only",
                  content and all("JavaScript Fundamentals" in h.get("subtitle", "") for h in content),
                  [h.get("subtitle") for h in content])
            r = call("GET", f"/api/v1/courses/{F['js']}/content/search", user, params={"q": "lesson"})
            check("in-course search works for the enrolled course", r.status == 200, r)
            r = call("GET", f"/api/v1/courses/{F['python']}/content/search", user, params={"q": "lesson"})
            check("in-course search is 403 for a course not enrolled", r.status == 403, r)
            r = call("GET", f"/api/v1/courses/{F['python']}/lessons", user)
            check("lesson listing of a course not enrolled is 403 (org affiliation is not staff)", r.status == 403, r)
        else:
            check("course_content shows nothing without an enrolment", not content, content)
        resp, recs = recommended(user)
        enrolled = {x[0] for x in sql(
            f"SELECT ce.course_uuid FROM course_enrollments ce JOIN students s ON s.uuid = ce.student_uuid "
            f"JOIN users u ON u.uuid = s.user_uuid WHERE u.email = {q(user + '@elimika.local')}")}
        check("recommendations answer", resp.status == 200 and recs, resp)
        check("recommendations exclude enrolled courses", not enrolled & {c["course_uuid"] for c in recs},
              names(recs))
        check("every recommendation carries reasons", all(c.get("reasons") for c in recs), names(recs))
        age = int(sql_value(f"SELECT date_part('year', age(dob)) FROM users WHERE email = {q(user + '@elimika.local')}"))
        too_young = {x[0] for x in sql(f"SELECT uuid FROM courses WHERE age_lower_limit > {age} "
                                        f"OR age_upper_limit < {age}")}
        check("recommendations respect the age band", not too_young & {c["course_uuid"] for c in recs})
        check("recommendations never include drafts/archived/shadows",
              not {F["draft"], F["archived"], F["shadow"]} & {c["course_uuid"] for c in recs})
        for kind, path in (("instructors", "/api/v1/instructors"), ("classes", "/api/v1/classes"),
                           ("jobs", "/api/v1/classes/jobs"), ("global", "/api/v1/search/instructors")):
            r = call("GET", path, user, params={"near": NEAR, "radius_km": 10})
            rows = page(r)
            ok = r.status == 200 and rows and all(row.get("distance_band") for row in rows)
            check(f"near-me {kind}: distance_band on every row", ok, r)
            check(f"near-me {kind}: no raw coordinates in the JSON", not raw_coordinates(r.text)
                  and "1.2921" not in r.text and "36.8219" not in r.text, raw_coordinates(r.text)[:5])
        r = call("GET", "/api/v1/instructors", None, params={"near": NEAR})
        check("near-me on a module listing needs a sign-in", r.status in (401, 403), r)

    # ------------------------------------------------------------------ minor
    section("Minor")
    resp, recs = recommended("qa-minor")
    check("minor gets recommendations", resp.status == 200 and recs, resp)
    check("the 18+ course is never recommended to the minor", F["trading"] not in {c["course_uuid"] for c in recs},
          names(recs))
    resp, recs = recommended("qa-minor", surface="next_steps")
    check("...nor on next_steps", F["trading"] not in {c["course_uuid"] for c in recs}, names(recs))
    pair = sql(f"SELECT shared_learners, includes_minors FROM course_co_enrolments WHERE "
               f"(course_uuid = {q(F['sql'])} AND neighbour_course_uuid = {q(F['web'])}) OR "
               f"(course_uuid = {q(F['web'])} AND neighbour_course_uuid = {q(F['sql'])})")
    check("pair involving the minor with < 10 learners is not stored (SQL+Web)", not pair, pair)
    bad = sql("SELECT course_uuid, neighbour_course_uuid, shared_learners FROM course_co_enrolments "
              "WHERE (includes_minors AND shared_learners < 10) OR shared_learners < 5")
    check("no stored pair breaks k>=5 / k>=10-with-minors", not bad, bad)
    adult_pair = sql(f"SELECT shared_learners FROM course_co_enrolments WHERE course_uuid = {q(F['js'])} "
                     f"AND neighbour_course_uuid = {q(F['python'])}")
    check("control: the 6-adult pair (JS+Python) is stored", bool(adult_pair), adult_pair)

    # ------------------------------------------------------------------ guardian
    section("Guardian")
    resp, recs = recommended("qa-guardian", student_uuid=F["minor"])
    check("guardian (ACADEMICS) reads the ward's recommendations", resp.status == 200 and recs, resp)
    check("...without the 18+ course", F["trading"] not in {c["course_uuid"] for c in recs})
    r = call("GET", f"/api/v1/students/{F['minor']}/skill-goals", "qa-guardian")
    check("guardian reads the ward's skill goals", r.status == 200 and r.data, r)
    r = call("GET", f"/api/v1/students/{F['minor']}/skill-goals", "qa-outsider")
    check("an unrelated learner cannot (403)", r.status == 403, r)
    resp, _ = recommended("qa-outsider", student_uuid=F["minor"])
    check("an unrelated learner cannot read the ward's recommendations (403)", resp.status == 403, resp)

    # ------------------------------------------------------------------ org manager
    section("Organisation manager")
    roster = f"/api/v1/organisations/{F['org']}/users"
    r = gsearch("qa-orgadmin", "sam", "people")
    check("people search finds an own member by name", "Sam Student" in names(hits(r, "people")), hits(r))
    r = gsearch("qa-orgadmin", "otto", "people")
    check("people search does not find a non-member", not hits(r, "people"), hits(r))
    r = gsearch("qa-orgadmin", "irene", "people")
    check("people search does not find another organisation's member", not hits(r, "people"), hits(r))
    r = gsearch("qa-orgadmin", "qa-student@elimika.local", "people")
    check("global people search is names only (no match on email)", not hits(r, "people"), hits(r))
    r = call("GET", roster, "qa-orgadmin", params={"q": "qa-student@elimika.local"})
    check("roster: exact email finds the member", [u.get("email") for u in page(r)] == ["qa-student@elimika.local"], r)
    r = call("GET", roster, "qa-orgadmin", params={"q": "qa-student@elim"})
    check("roster: a partial address finds nothing", r.status == 200 and not page(r), r)
    r = call("GET", roster, "qa-orgadmin", params={"q": "qa-outsider@elimika.local"})
    check("roster: a non-member's exact email finds nothing", r.status == 200 and not page(r), r)
    r = call("GET", f"/api/v1/organisations/{F['org2']}/users", "qa-orgadmin", params={"q": "Irene"})
    check("roster of another organisation is 403", r.status == 403, r)
    # Revoke in the DB only (no entity event, so the index still lists the member): the SQL re-check must drop them.
    mapping = sql_value(f"SELECT m.uuid FROM user_organisation_domain_mapping m WHERE m.organisation_uuid = "
                        f"{q(F['org'])} AND m.user_uuid = {q(F['student_user'])} AND m.active AND NOT m.deleted")
    if check("member's active mapping exists before revoke", bool(mapping)):
        try:
            sql(f"UPDATE user_organisation_domain_mapping SET active = false WHERE uuid = {q(mapping)}")
            r = gsearch("qa-orgadmin", "sam", "people")
            check("revoked member disappears from people search immediately", not hits(r, "people"), hits(r))
            r = call("GET", roster, "qa-orgadmin", params={"q": "sam"})
            check("revoked member disappears from the roster search immediately", r.status == 200 and not page(r), r)
        finally:
            sql(f"UPDATE user_organisation_domain_mapping SET active = true WHERE uuid = {q(mapping)}")
    r = call("GET", f"/api/v1/classes/jobs/{F['job_open']}/candidates", "qa-orgadmin")
    items = (r.data or {}).get("items", []) if r.status == 200 else []
    check("candidates answer for the posting organisation", r.status == 200 and items, r)
    item_keys = {k for i in items for k in i}
    match_keys = {k for i in items for k in i.get("match", {})}
    check("candidates carry only the fit-summary fields",
          item_keys <= {"instructor_uuid", "display_name", "location_name", "admin_verified", "match"}
          and match_keys <= {"score", "reasons", "schedule_clear", "rate_within_budget"}, (item_keys, match_keys))
    check("candidates never show rates, clashes or pay",
          not re.search(r"approved_rate|instructor_pay|schedule_conflicts|hourly_rate", r.text), r.text[:200])
    r = call("GET", f"/api/v1/classes/jobs/{F['job_open']}/candidates", "qa-student")
    check("candidates are 403 for a learner", r.status == 403, r)
    r = call("GET", f"/api/v1/classes/jobs/{F['job_open']}/candidates", "qa-creator2")
    check("candidates are 403 for another organisation's manager", r.status == 403, r)
    r = call("GET", "/api/v1/classes/jobs", "qa-orgadmin", params={"q": "trainer", "organisation_uuid": F["org"]})
    check("own organisation sees its non-OPEN (FILLED) job", "filled" in [j.get("status") for j in page(r)], r)
    r = call("GET", "/api/v1/classes/jobs", "qa-student", params={"q": "trainer", "organisation_uuid": F["org"]})
    check("a learner sees only OPEN jobs", r.status == 200 and all(j.get("status") == "open" for j in page(r)), r)
    r = gsearch("qa-student", "SQL Evening Trainer", "marketplace_jobs")
    check("a learner's global search hides the FILLED job", "SQL Evening Trainer" not in names(hits(r)), hits(r))

    # ------------------------------------------------------------------ instructor
    section("Instructor")
    r = call("GET", "/api/v1/classes/jobs/matches", "qa-instructor")
    items = (r.data or {}).get("items", []) if r.status == 200 else []
    check("best matches answer", r.status == 200 and items, r)
    check("every match has eligibility and reasons",
          all(i.get("match", {}).get("eligibility") is not None and i["match"].get("reasons") for i in items),
          [i.get("title") for i in items])
    elig = [i["match"]["eligibility"]["eligible"] for i in items]
    check("ineligible matches are ranked last", elig == sorted(elig, reverse=True), elig)
    check("matches only OPEN jobs", all(i.get("status") == "open" for i in items), [i.get("status") for i in items])
    r = call("GET", "/api/v1/classes/jobs/matches", "qa-student")
    check("matches are 403 for a learner", r.status == 403, r)
    for user in ("qa-student", "qa-orgadmin", "qa-instructor"):
        r = call("GET", "/api/v1/instructors", user, params={"q": "Irene"})
        check(f"unverified instructor hidden from {user}", r.status == 200 and not page(r), r)
    r = gsearch("qa-student", "irene", "instructors")
    check("unverified instructor hidden from global search", not hits(r, "instructors"), hits(r))
    r = call("GET", "/api/v1/instructors", "qa-admin", params={"q": "Irene"})
    check("control: platform admin sees the unverified instructor", len(page(r)) == 1, r)
    r = call("GET", "/api/v1/search/instructors", "qa-student", params={"near": NEAR, "radius_km": 50})
    check("unverified (opted-in) instructor never appears near-me",
          "Irene" not in json.dumps(r.json), names(page(r)))
    try:
        r = call("PUT", f"/api/v1/instructors/{F['inst']}/location-search", "qa-instructor", {"enabled": False})
        check("instructor opts out of location search", r.status == 200, r)
        def gone():
            a = call("GET", "/api/v1/instructors", "qa-student", params={"near": NEAR, "radius_km": 50})
            b = call("GET", "/api/v1/search/instructors", "qa-student", params={"near": NEAR, "radius_km": 50})
            return a.status == 200 and not page(a) and b.status == 200 and not page(b)
        check("opted-out instructor never appears near-me (listing and global)", wait_for(gone, 20, 1))
        r = call("GET", "/api/v1/instructors", "qa-student", params={"q": "Ian"})
        check("...but is still listed without near", len(page(r)) == 1, r)
    finally:
        call("PUT", f"/api/v1/instructors/{F['inst']}/location-search", "qa-instructor", {"enabled": True})
        wait_for(lambda: bool(page(call("GET", "/api/v1/instructors", "qa-student",
                                        params={"near": NEAR, "radius_km": 50}))), 20, 1)
    r = gsearch("qa-instructor", "js", "marketplace_jobs")
    if hits(r, "marketplace_jobs"):
        check("q=js finds the JavaScript-tagged job through the alias", True)
    else:
        pending("q=js finds the JavaScript-tagged job through the alias",
                "skill aliases are not wired into Meilisearch synonyms on this base (skills-taxonomy.md)")

    # ------------------------------------------------------------------ creator
    section("Course creator")
    r = call("GET", "/api/v1/courses", "qa-creator", params={"q": "rust"})
    check("own draft is visible to its creator", F["draft"] in [c["uuid"] for c in page(r)], r)
    r = call("GET", "/api/v1/courses", "qa-creator2", params={"q": "rust"})
    check("another creator's draft is not", r.status == 200 and F["draft"] not in [c["uuid"] for c in page(r)], r)
    r = call("PUT", f"/api/v1/courses/{F['js']}/prerequisites", "qa-creator",
             {"prerequisites": [{"prerequisite_course_uuid": F["js_adv"]}]})
    check("a prerequisite cycle is 400", r.status == 400 and "cycle" in r.text, r)
    current = [{"skill_uuid": s, "level": lv.lower(), "weight": int(w)} for s, lv, w in
               sql(f"SELECT skill_uuid, level, weight FROM course_skills WHERE course_uuid = {q(F['js'])}")]
    r = call("PUT", f"/api/v1/courses/{F['js']}/skills", "qa-creator2", {"skills": []})
    check("skill tagging by another creator is 403", r.status == 403, r)
    r = call("PUT", f"/api/v1/courses/{F['js']}/skills", "qa-creator", {"skills": current})
    check("skill tagging by the owner works", r.status == 200, r)
    r = call("GET", "/api/v1/rubrics/search", "qa-creator", params={"q": "rubric"})
    titles = names(page(r), "title")
    check("creator sees public + own private rubrics", {"Public Project Rubric", "Private Chloe Rubric"} <= set(titles),
          titles)
    check("creator never sees another creator's private rubric", "Private Carl Rubric" not in titles, titles)
    r = gsearch("qa-creator2", "rubric", "rubrics")
    check("global search: the other creator sees their own, not Chloe's private",
          "Private Chloe Rubric" not in names(hits(r)) and "Private Carl Rubric" in names(hits(r)), names(hits(r)))
    r = call("GET", "/api/v1/rubrics/search", "qa-instructor", params={"q": "rubric"})
    check("instructor sees public rubrics only", names(page(r), "title") == ["Public Project Rubric"], r)
    r = gsearch("qa-student", "rubric")
    check("learner's global search hides rubrics", "rubrics" not in totals(r), totals(r))

    # ------------------------------------------------------------------ admin
    section("Platform admin")
    r = gsearch("qa-admin", "rubric", "rubrics")
    check("admin sees every rubric", len(hits(r, "rubrics")) == 3, names(hits(r)))
    r = gsearch("qa-admin", "rust", "courses")
    check("admin sees drafts", F["draft"] in [h["uuid"] for h in hits(r)], hits(r))
    r = gsearch("qa-admin", "otto", "people")
    check("admin sees everyone in people search", bool(hits(r, "people")), hits(r))
    r = gsearch("qa-admin", "a")
    check("q shorter than 2 characters is 400", r.status == 400, r)
    r = call("GET", "/api/v1/users/search", "qa-admin", params={"q": "qa-outsider@elimika.local"})
    check("admin /users/search matches email", r.status == 200 and page(r), r)
    r = call("GET", "/api/v1/users/search", "qa-student", params={"q": "otto"})
    check("/users/search is 403 for a learner", r.status == 403, r)
    r = call("GET", "/api/v1/admin/search/indexes", "qa-admin")
    states = {i["index_name"]: i["status"] for i in (r.data or [])}
    check("index admin: every index READY", r.status == 200 and states and set(states.values()) == {"READY"}, states)
    r = call("POST", f"/api/v1/admin/search/indexes/courses/documents/{F['js']}/sync", "qa-admin")
    check("index admin: single-document sync accepted", r.status in (200, 202), r)
    r = call("GET", "/api/v1/admin/search/indexes", "qa-student")
    check("index admin is 403 for a learner", r.status == 403, r)
    r = call("GET", "/api/v1/admin/recommendations/evaluation", "qa-admin")
    check("recommender evaluation answers for admin", r.status == 200, r)

    # ------------------------------------------------------------------ audit log
    section("Audit log")
    marker = "zqxjkredactme"
    gsearch(None, marker)
    call("GET", "/api/v1/instructors", "qa-student", params={"near": NEAR})
    time.sleep(1.5)
    leaked = sql(f"SELECT request_uri, query_string FROM request_audit_log WHERE query_string LIKE {q('%' + marker + '%')} "
                 f"OR query_string LIKE '%1.2921%'")
    check("query text and near point never reach request_audit_log.query_string", not leaked, leaked)
    row = sql_value("SELECT query_string FROM request_audit_log WHERE request_uri = '/api/v1/search' "
                    "ORDER BY id DESC LIMIT 1")
    check("q is stored redacted", row is not None and "q=[redacted:" in row, row)

    # ------------------------------------------------------------------ search down
    section("Search down (Meilisearch stopped)")
    try:
        compose("stop", "meilisearch")
        r = call("GET", "/api/v1/courses", "qa-student", params={"q": "java"})
        check("q on a module listing is 503 'Search is unavailable'",
              r.status == 503 and (r.json or {}).get("message") == "Search is unavailable", r)
        r = gsearch(None, "java")
        check("global search is 503 'Search is unavailable'",
              r.status == 503 and (r.json or {}).get("message") == "Search is unavailable", r)
        r = call("GET", "/api/v1/courses", "qa-student")
        check("listing without q still works from PostgreSQL", r.status == 200 and page(r), r)
        r = call("GET", f"/api/v1/courses/{F['js']}/similar")
        check("/similar still answers (SQL candidates)", r.status == 200, r)
        r = call("GET", "/api/v1/courses/recommendations", "qa-student")
        check("recommendations still answer (search off: SQL candidates)", r.status == 200 and r.data, r)
    finally:
        compose("start", "meilisearch")
        recovered = wait_for(lambda: gsearch(None, "javascript").status == 200, 90, 2)
    check("search recovers after Meilisearch restarts", recovered)

    # ------------------------------------------------------------------ summary
    passed = sum(1 for s, _, _ in RESULTS if s == "PASS")
    failed = [(n, d) for s, n, d in RESULTS if s == "FAIL"]
    pend = [n for s, n, _ in RESULTS if s == "PENDING"]
    print(f"\nSUMMARY: {passed} passed, {len(failed)} failed, {len(pend)} pending")
    for n, d in failed:
        print(f"  FAIL {n}: {str(d)[:300]}")
    for n in pend:
        print(f"  PENDING {n}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
