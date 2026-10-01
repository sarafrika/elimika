#!/usr/bin/env python3
"""Idempotent seed for the local stack. Run through scripts/local/seed.sh.

Uses the application's own API (as the matching QA user, or qa-admin) wherever the API can do it, so the data
goes through the same validation, events and indexing as real traffic. SQL is used only for what the API cannot
do in a seed: synthetic co-enrolment learners (they have no Keycloak account), enrolment history, and a FILLED job.
Every step first looks for what it would create, so re-running changes nothing.
"""
import datetime as dt
import os
import sys
import time

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "lib"))
from elimika_local import call, must, sql, sql_value, q, token  # noqa: E402

QA_USERS = ["qa-admin", "qa-orgadmin", "qa-instructor", "qa-instructor2", "qa-creator", "qa-creator2",
            "qa-student", "qa-minor", "qa-guardian", "qa-outsider"]
NAIROBI_BRANCH = (-1.2676, 36.8108)      # Westlands
NAIROBI_CLASS = (-1.2841, 36.8155)       # CBD
INSTRUCTOR_POINT = (-1.2921, 36.8219)    # about 1-2 km from both


def log(msg):
    print(f"[seed] {msg}", flush=True)


def user_uuid(user):
    return sql_value(f"SELECT uuid FROM users WHERE lower(email) = lower({q(user + '@elimika.local')})")


def ensure_users():
    """A user row appears on a user's first authenticated request (UserSyncFilter)."""
    for u in QA_USERS:
        must(call("GET", "/api/v1/users/me", u) if False else call("GET", "/api/v1/users", u, params={"size": 1}),
             f"first request as {u}", ok=(200, 403))
    return {u: user_uuid(u) for u in QA_USERS}


# ---------------------------------------------------------------- profiles

def ensure_student(u, uid, guardian_name=None):
    existing = sql_value(f"SELECT uuid FROM students WHERE user_uuid = {q(uid)}")
    if existing:
        return existing
    body = {"user_uuid": uid, "bio": f"Local QA learner {u}"}
    if guardian_name:
        body.update({"first_guardian_name": guardian_name, "first_guardian_mobile": "+254700000008"})
    must(call("POST", "/api/v1/students", u, body), f"student profile {u}")
    return sql_value(f"SELECT uuid FROM students WHERE user_uuid = {q(uid)}")


def ensure_instructor(u, uid, point=None, headline="Instructor"):
    existing = sql_value(f"SELECT uuid FROM instructors WHERE user_uuid = {q(uid)}")
    if existing:
        return existing
    body = {"user_uuid": uid, "professional_headline": headline, "bio": f"Local QA instructor {u}",
            "location_name": "Nairobi"}
    if point:
        body.update({"latitude": point[0], "longitude": point[1]})
    must(call("POST", "/api/v1/instructors", u, body), f"instructor profile {u}")
    return sql_value(f"SELECT uuid FROM instructors WHERE user_uuid = {q(uid)}")


def ensure_creator(u, uid, name):
    existing = sql_value(f"SELECT uuid FROM course_creators WHERE user_uuid = {q(uid)}")
    if existing:
        return existing
    must(call("POST", "/api/v1/course-creators", u, {"user_uuid": uid, "full_name": name,
                                                     "professional_headline": "Course author"}),
         f"course creator profile {u}")
    return sql_value(f"SELECT uuid FROM course_creators WHERE user_uuid = {q(uid)}")


def verify_profiles(instructors, creators):
    if sql_value(f"SELECT admin_verified FROM instructors WHERE uuid = {q(instructors['qa-instructor'])}") != "t":
        must(call("POST", f"/api/v1/admin/instructors/{instructors['qa-instructor']}/verify", "qa-admin",
                  params={"reason": "local seed"}), "verify qa-instructor")
    for c in creators.values():
        if sql_value(f"SELECT admin_verified FROM course_creators WHERE uuid = {q(c)}") != "t":
            must(call("POST", f"/api/v1/course-creators/{c}/verify", "qa-admin", params={"reason": "local seed"}),
                 "verify creator")
    # Both instructors opt in; only the verified one can ever be located (the unverified one has no point).
    for u, i in instructors.items():
        if sql_value(f"SELECT location_search_opt_in FROM instructors WHERE uuid = {q(i)}") != "t":
            must(call("PUT", f"/api/v1/instructors/{i}/location-search", u, {"enabled": True}), f"opt in {u}")


# ---------------------------------------------------------------- organisations

def ensure_org(name, creator_user, location, point):
    existing = sql_value(f"SELECT uuid FROM organisation WHERE name = {q(name)} AND NOT deleted")
    if not existing:
        must(call("POST", "/api/v1/organisations", creator_user, {
            "name": name, "active": True, "description": f"{name} (local seed)", "location": location,
            "country": "Kenya", "latitude": point[0], "longitude": point[1]}), f"create org {name}")
        existing = sql_value(f"SELECT uuid FROM organisation WHERE name = {q(name)} AND NOT deleted")
    if sql_value(f"SELECT admin_verified FROM organisation WHERE uuid = {q(existing)}") != "t":
        must(call("POST", f"/api/v1/admin/organisations/{existing}/moderate", "qa-admin",
                  params={"action": "approve", "reason": "local seed"}), f"approve org {name}")
    return existing


def ensure_branch(org, name, point, manager):
    existing = sql_value(f"SELECT uuid FROM training_branches WHERE organisation_uuid = {q(org)} "
                         f"AND branch_name = {q(name)} AND NOT deleted")
    if existing:
        return existing
    must(call("POST", f"/api/v1/organisations/{org}/training-branches", manager, {
        "organisation_uuid": org, "branch_name": name, "address": f"{name}, Nairobi",
        "latitude": point[0], "longitude": point[1], "poc_name": "Oscar Manager",
        "poc_email": "qa-orgadmin@elimika.local", "poc_telephone": "+254700000002", "active": True}),
         f"branch {name}")
    return sql_value(f"SELECT uuid FROM training_branches WHERE organisation_uuid = {q(org)} "
                     f"AND branch_name = {q(name)} AND NOT deleted")


def ensure_membership(org, user_uuid_, domain, branch, actor):
    existing = sql_value(
        f"SELECT m.uuid FROM user_organisation_domain_mapping m JOIN user_domain d ON d.uuid = m.domain_uuid "
        f"WHERE m.organisation_uuid = {q(org)} AND m.user_uuid = {q(user_uuid_)} AND d.domain_name = {q(domain)} "
        f"AND m.active AND NOT m.deleted")
    if existing:
        return existing
    body = {"domain_name": domain}
    if branch:
        body["branch_uuid"] = branch
    must(call("PUT", f"/api/v1/organisations/{org}/users/{user_uuid_}/domain", actor, body),
         f"membership {domain} in {org}")


# ---------------------------------------------------------------- reference data

def ensure_category(name, parent=None):
    existing = sql_value(f"SELECT uuid FROM course_categories WHERE name = {q(name)}")
    if existing:
        return existing
    body = {"name": name, "description": f"{name} courses", "is_active": True}
    if parent:
        body["parent_uuid"] = parent
    return must(call("POST", "/api/v1/config/categories", "qa-admin", body), f"category {name}")["uuid"]


def ensure_difficulty(name, order):
    existing = sql_value(f"SELECT uuid FROM course_difficulty_levels WHERE name = {q(name)}")
    if existing:
        return existing
    return must(call("POST", "/api/v1/config/difficulty-levels", "qa-admin",
                     {"name": name, "level_order": order, "description": name}), f"difficulty {name}")["uuid"]


def ensure_skill(name, aliases=()):
    slug = name.lower().replace(" ", "-")
    existing = sql_value(f"SELECT uuid FROM skills WHERE slug = {q(slug)}")
    if existing:
        return existing
    return must(call("POST", "/api/v1/admin/skills", "qa-admin",
                     {"name": name, "aliases": list(aliases), "active": True}), f"skill {name}")["uuid"]


# ---------------------------------------------------------------- courses

TEXT_CONTENT_TYPE = "Text"


def course_row(name):
    rows = sql(f"SELECT uuid, status, admin_approved, active FROM courses WHERE name = {q(name)} "
               f"AND parent_course_uuid IS NULL ORDER BY id LIMIT 1")
    return rows[0] if rows else None


def ensure_course(owner, creator_uuid, name, description, categories, level, age_lower=None, lessons=2):
    row = course_row(name)
    if not row:
        body = {
            "name": name, "course_creator_uuid": creator_uuid, "category_uuids": categories,
            "difficulty_uuid": level, "description": description,
            "objectives": f"Understand {name}", "duration_hours": 4, "duration_minutes": 0,
            "creator_share_percentage": 60, "instructor_share_percentage": 40, "status": "draft",
            "active": False, "price": 0, "minimum_training_fee": 100, "class_limit": 30,
        }
        if age_lower is not None:
            body["age_lower_limit"] = age_lower
        must(call("POST", "/api/v1/courses", owner, body), f"create course {name}")
        row = course_row(name)
    uuid = row[0]
    content_type = sql_value(f"SELECT uuid FROM lesson_content_types WHERE name = {q(TEXT_CONTENT_TYPE)}")
    for n in range(1, lessons + 1):
        title = f"{name} - Lesson {n}"
        lesson = sql_value(f"SELECT uuid FROM lessons WHERE course_uuid = {q(uuid)} AND title = {q(title)}")
        if not lesson:
            lesson = must(call("POST", f"/api/v1/courses/{uuid}/lessons", owner, {
                "course_uuid": uuid, "lesson_number": n, "title": title, "status": "PUBLISHED", "active": True,
                "description": f"Lesson {n} of {name}", "learning_objectives": "Practice the basics"}),
                f"lesson {title}")["uuid"]
        if not sql_value(f"SELECT uuid FROM lesson_contents WHERE lesson_uuid = {q(lesson)}"):
            must(call("POST", f"/api/v1/courses/{uuid}/lessons/{lesson}/content", owner, {
                "lesson_uuid": lesson, "content_type_uuid": content_type, "title": f"Reading for {title}",
                "description": "Read this first", "is_required": True,
                "content_text": f"<p>Notes about {name}: variables, functions and loops.</p>"}),
                f"content for {title}")
        if n == 1 and not sql_value(f"SELECT uuid FROM quizzes WHERE lesson_uuid = {q(lesson)}"):
            must(call("POST", "/api/v1/quizzes", owner, {
                "lesson_uuid": lesson, "title": f"{name} checkpoint quiz", "description": "Check yourself",
                "instructions": "Answer every question", "attempts_allowed": 3, "passing_score": 50,
                "status": "PUBLISHED", "active": True}), f"quiz for {title}")
        if n == 1 and not sql_value(f"SELECT uuid FROM assignments WHERE lesson_uuid = {q(lesson)}"):
            must(call("POST", "/api/v1/assignments", owner, {
                "lesson_uuid": lesson, "title": f"{name} practical assignment", "description": "Build it",
                "instructions": "Submit a short write-up", "max_points": 100, "submission_types": ["TEXT"],
                "is_published": True}), f"assignment for {title}")
    return uuid


def publish_and_approve(owner, uuid, name):
    row = course_row(name)
    if row[1] in ("draft", "in_review"):
        must(call("POST", f"/api/v1/courses/{uuid}/publish", owner), f"publish {name}")
        row = course_row(name)
    if row[2] != "t":
        must(call("POST", f"/api/v1/admin/courses/{uuid}/moderate", "qa-admin",
                  {"action": "approved", "reason": "local seed"}), f"approve {name}")


def ensure_prerequisites(owner, uuid, prereqs):
    current = {r[0] for r in sql(f"SELECT prerequisite_course_uuid FROM course_prerequisites "
                                 f"WHERE course_uuid = {q(uuid)}")}
    if current != set(prereqs):
        must(call("PUT", f"/api/v1/courses/{uuid}/prerequisites", owner, {
            "prerequisites": [{"prerequisite_course_uuid": p, "is_mandatory": True} for p in prereqs]}),
            f"prerequisites of {uuid}")


def ensure_course_skills(owner, uuid, items):
    current = {r[0] for r in sql(f"SELECT skill_uuid FROM course_skills WHERE course_uuid = {q(uuid)}")}
    if current != {s for s, _, _ in items}:
        must(call("PUT", f"/api/v1/courses/{uuid}/skills", owner, {
            "skills": [{"skill_uuid": s, "level": lv, "weight": w} for s, lv, w in items]}), f"skills of {uuid}")


# ---------------------------------------------------------------- rubrics, approvals, classes, jobs

def ensure_rubric(owner, creator_uuid, title, public):
    existing = sql_value(f"SELECT uuid FROM assessment_rubrics WHERE title = {q(title)}")
    if existing:
        return existing
    return must(call("POST", "/api/v1/rubrics", owner, {
        "title": title, "description": f"{title} (local seed)", "rubric_type": "Project",
        "course_creator_uuid": creator_uuid, "is_public": public, "status": "PUBLISHED", "active": True,
        "total_weight": 100, "weight_unit": "percentage", "uses_custom_levels": False, "max_score": 100,
        "min_passing_score": 50}), f"rubric {title}")["uuid"]


RATE = 500


def rate_card():
    card = {"currency": "KES"}
    for fmt in ("private", "group"):
        for loc in ("online", "inperson"):
            for basis in ("hourly", "session", "daily"):
                card[f"{fmt}_{loc}_{basis}_rate"] = RATE
    return card


def ensure_training_approval(course_uuid, instructor_user, instructor_uuid, approve=True, applicant_type="instructor"):
    row = sql(f"SELECT uuid, status FROM course_training_applications WHERE course_uuid = {q(course_uuid)} "
              f"AND applicant_uuid = {q(instructor_uuid)}")
    if not row:
        app = must(call("POST", f"/api/v1/courses/{course_uuid}/training-applications", instructor_user, {
            "applicant_type": applicant_type, "applicant_uuid": instructor_uuid, "rate_card": rate_card(),
            "application_notes": "Local seed application"}), f"training application {course_uuid}")
        row = [[app["uuid"], app.get("status", "pending")]]
    app_uuid, status = row[0]
    if approve and status.lower() != "approved":
        must(call("POST", f"/api/v1/courses/{course_uuid}/training-applications/{app_uuid}", "qa-creator",
                  {"review_notes": "Approved by local seed"}, params={"action": "approve"}),
             f"approve application {app_uuid}")


def iso(day_offset, hour, minute=0):
    d = dt.datetime.now(dt.timezone.utc).replace(hour=hour, minute=minute, second=0, microsecond=0)
    return (d + dt.timedelta(days=day_offset)).strftime("%Y-%m-%dT%H:%M:%SZ")


def day(day_offset):
    return (dt.date.today() + dt.timedelta(days=day_offset)).isoformat()


def ensure_class(actor, title, course_uuid, org, branch, instructor_uuid, visibility, location_type, point,
                 start_day, hour):
    existing = sql_value(f"SELECT uuid FROM class_definitions WHERE title = {q(title)}")
    if existing:
        return existing
    body = {
        "title": title, "description": f"{title} (local seed)", "default_instructor_uuid": instructor_uuid,
        "organisation_uuid": org, "branch_uuid": branch, "course_uuid": course_uuid,
        "class_visibility": visibility, "session_format": "GROUP", "location_type": location_type,
        "default_start_time": iso(start_day, hour), "default_end_time": iso(start_day, hour + 2),
        "registration_period_start_date": day(-30), "registration_period_end_date": day(start_day + 20),
        "academic_period_start_date": day(start_day), "academic_period_end_date": day(start_day + 30),
        "max_participants": 30, "allow_waitlist": False, "is_active": True,
        "sale_price": 1000, "instructor_pay": RATE, "rate_basis": "per_hour",
        "session_templates": [{
            "start_time": iso(start_day, hour), "end_time": iso(start_day, hour + 2), "timezone": "Africa/Nairobi",
            "conflict_resolution": "SKIP",
            "recurrence": {"recurrence_type": "WEEKLY", "interval_value": 1, "occurrence_count": 4,
                           "days_of_week": dt.date.fromisoformat(day(start_day)).strftime("%A").upper()},
        }],
    }
    if location_type == "ONLINE":
        body["meeting_link"] = "https://meet.example.invalid/elimika-local"
    else:
        body.update({"location_name": "Nairobi CBD, Kimathi Street", "location_latitude": point[0],
                     "location_longitude": point[1]})
    must(call("POST", "/api/v1/classes", actor, body), f"class {title}")
    return sql_value(f"SELECT uuid FROM class_definitions WHERE title = {q(title)}")


def ensure_job(actor, title, org, branch, course_uuid, location_type, point, start_day, hour):
    existing = sql_value(f"SELECT uuid FROM class_marketplace_jobs WHERE title = {q(title)}")
    if existing:
        return existing
    body = {
        "organisation_uuid": org, "branch_uuid": branch, "course_uuid": course_uuid, "title": title,
        "description": f"{title} (local seed)", "class_visibility": "PUBLIC", "session_format": "GROUP",
        "default_start_time": iso(start_day, hour), "default_end_time": iso(start_day, hour + 2),
        "registration_period_start_date": day(0), "registration_period_end_date": day(start_day - 1),
        "academic_period_start_date": day(start_day), "academic_period_end_date": day(start_day + 30),
        "location_type": location_type, "max_participants": 20, "allow_waitlist": False,
        "sale_price": 1000, "instructor_pay": 800, "rate_basis": "per_hour",
        "target_groups": ["adults"],
        "session_templates": [{"start_time": iso(start_day, hour), "end_time": iso(start_day, hour + 2),
                               "timezone": "Africa/Nairobi", "conflict_resolution": "SKIP"}],
    }
    if location_type == "ONLINE":
        body["meeting_link"] = "https://meet.example.invalid/elimika-job"
    else:
        body.update({"location_name": "Westlands, Nairobi", "location_latitude": point[0],
                     "location_longitude": point[1]})
    must(call("POST", "/api/v1/classes/jobs", actor, body), f"job {title}")
    return sql_value(f"SELECT uuid FROM class_marketplace_jobs WHERE title = {q(title)}")


def ensure_job_required_skills(actor, job, items):
    current = {r[0] for r in sql(f"SELECT skill_uuid FROM class_marketplace_job_required_skills "
                                 f"WHERE job_uuid = {q(job)}")}
    if current != {s for s, _, _ in items}:
        must(call("PUT", f"/api/v1/classes/jobs/{job}/required-skills", actor, {
            "skills": [{"skill_uuid": s, "min_proficiency": p, "is_mandatory": m} for s, p, m in items]}),
            f"required skills {job}")


# ---------------------------------------------------------------- learners, enrolments, reviews

def ensure_class_enrolment(student_user, student_uuid, class_uuid, course_uuid):
    if sql_value(f"SELECT uuid FROM course_enrollments WHERE student_uuid = {q(student_uuid)} "
                 f"AND course_uuid = {q(course_uuid)}"):
        return
    must(call("POST", "/api/v1/enrollment", student_user,
              {"class_definition_uuid": class_uuid, "student_uuid": student_uuid}), f"enrol {student_user}")


def ensure_synthetic_learners(prefix, count, dob):
    """Learners without a Keycloak account: they only exist to give the co-enrolment job real pairs."""
    out = []
    for n in range(1, count + 1):
        mail = f"{prefix}{n:02d}@learners.elimika.local"
        uid = sql_value(f"SELECT uuid FROM users WHERE email = {q(mail)}")
        if not uid:
            # users_user_no_format wants 9 digits; the 99x range stays clear of UserNumberService's sequence.
            user_no = f"99{len(prefix) % 10}{n:06d}"
            uid = sql_value(
                f"INSERT INTO users (first_name, last_name, email, username, active, created_by, user_no, dob) "
                f"VALUES ({q(prefix.title())}, {q('Learner ' + str(n))}, {q(mail)}, {q(mail)}, true, 'local-seed', "
                f"{q(user_no)}, {q(dob)}) RETURNING uuid")
        sid = sql_value(f"SELECT uuid FROM students WHERE user_uuid = {q(uid)}")
        if not sid:
            sid = sql_value(f"INSERT INTO students (full_name, user_uuid, created_by) VALUES "
                            f"({q(prefix.title() + ' Learner ' + str(n))}, {q(uid)}, 'local-seed') RETURNING uuid")
        out.append(sid)
    return out


def ensure_course_enrolment_sql(student_uuid, course_uuid, status="active", progress=40, days_ago=10):
    completion = f"now() - interval '{max(days_ago - 3, 0)} days'" if status == "completed" else "NULL"
    sql(f"INSERT INTO course_enrollments (student_uuid, course_uuid, status, progress_percentage, enrollment_date, "
        f"completion_date, created_date, created_by) VALUES ({q(student_uuid)}, {q(course_uuid)}, {q(status)}, "
        f"{100 if status == 'completed' else progress}, now() - interval '{days_ago} days', {completion}, "
        f"now() - interval '{days_ago} days', 'local-seed') ON CONFLICT (student_uuid, course_uuid) DO NOTHING")


def ensure_review(student_user, student_uuid, course_uuid, rating, headline):
    if sql_value(f"SELECT uuid FROM course_reviews WHERE student_uuid = {q(student_uuid)} "
                 f"AND course_uuid = {q(course_uuid)}"):
        return
    must(call("POST", f"/api/v1/courses/{course_uuid}/reviews", student_user, {
        "course_uuid": course_uuid, "student_uuid": student_uuid, "rating": rating, "headline": headline,
        "comments": "Seeded review", "is_anonymous": False}), f"review by {student_user}")


def ensure_guardian_link(minor_student, guardian_user_uuid):
    if sql_value(f"SELECT uuid FROM student_guardian_links WHERE student_uuid = {q(minor_student)} "
                 f"AND guardian_user_uuid = {q(guardian_user_uuid)} AND link_status = 'ACTIVE'"):
        return
    must(call("POST", "/api/v1/guardians/links", "qa-admin", {
        "studentUuid": minor_student, "guardianUserUuid": guardian_user_uuid, "relationshipType": "PARENT",
        "shareScope": "ACADEMICS", "isPrimary": True, "notes": "Local seed"}), "guardian link")


def ensure_skill_goals(student_user, student_uuid, skill_uuids):
    current = {r[0] for r in sql(f"SELECT skill_uuid FROM learner_skill_goals WHERE student_uuid = {q(student_uuid)}")}
    if current != set(skill_uuids):
        must(call("PUT", f"/api/v1/students/{student_uuid}/skill-goals", student_user,
                  {"skill_uuids": skill_uuids}), f"skill goals {student_user}")


# ---------------------------------------------------------------- search + features

def refresh_features():
    out = must(call("POST", "/api/v1/admin/recommendations/features/refresh", "qa-admin"), "feature refresh")
    log(f"course features refreshed: {out}")


def rebuild_indexes_and_wait(timeout=300):
    started = dt.datetime.now(dt.timezone.utc)
    must(call("POST", "/api/v1/admin/search/rebuild", "qa-admin"), "search rebuild")
    deadline = time.time() + timeout
    states = {}
    while time.time() < deadline:
        time.sleep(2)
        indexes = must(call("GET", "/api/v1/admin/search/indexes", "qa-admin"), "index status")
        states = {}
        for i in indexes:
            built = i.get("last_built_at")
            fresh = built and dt.datetime.fromisoformat(built.replace("Z", "+00:00")) >= started
            in_sync = i.get("engine_document_count") == i.get("recorded_document_count")
            states[i["index_name"]] = (i["status"] if fresh and in_sync and not i.get("engine_indexing")
                                       else f"{i['status']}(pending)")
        if states and all(s == "READY" for s in states.values()):
            log("indexes READY: " + ", ".join(f"{i['index_name']}={i['engine_document_count']}" for i in indexes))
            return indexes
    raise SystemExit(f"indexes not READY after {timeout}s: {states}")


def main():
    t0 = time.time()
    users = ensure_users()
    log(f"users synced: {len(users)}")

    students = {
        "qa-student": ensure_student("qa-student", users["qa-student"]),
        "qa-minor": ensure_student("qa-minor", users["qa-minor"], guardian_name="Grace Guardian"),
        "qa-outsider": ensure_student("qa-outsider", users["qa-outsider"]),
    }
    instructors = {
        "qa-instructor": ensure_instructor("qa-instructor", users["qa-instructor"], INSTRUCTOR_POINT,
                                           "Python and JavaScript trainer"),
        "qa-instructor2": ensure_instructor("qa-instructor2", users["qa-instructor2"], INSTRUCTOR_POINT,
                                            "Data trainer (unverified)"),
    }
    creators = {
        "qa-creator": ensure_creator("qa-creator", users["qa-creator"], "Chloe Creator"),
        "qa-creator2": ensure_creator("qa-creator2", users["qa-creator2"], "Carl Othercreator"),
    }
    log(f"profiles: students={students} instructors={instructors} creators={creators}")
    verify_profiles(instructors, creators)

    org = ensure_org("Nairobi Code Academy", "qa-orgadmin", "Nairobi", NAIROBI_BRANCH)
    org2 = ensure_org("Mombasa Data School", None, "Mombasa", (-4.0435, 39.6682))
    branch = ensure_branch(org, "Westlands Campus", NAIROBI_BRANCH, "qa-orgadmin")
    ensure_membership(org, users["qa-instructor"], "instructor", branch, "qa-orgadmin")
    ensure_membership(org2, users["qa-instructor2"], "instructor", None, "qa-admin")
    # The second organisation's manager is qa-creator2 (granted by the platform admin).
    ensure_membership(org2, users["qa-creator2"], "admin", None, "qa-admin")
    log(f"orgs: {org} (branch {branch}), {org2}")

    cat_prog = ensure_category("Programming")
    cat_web = ensure_category("Web Development", cat_prog)
    cat_data = ensure_category("Data Science")
    lvl = {n: ensure_difficulty(n, o) for n, o in (("Beginner", 1), ("Intermediate", 2), ("Advanced", 3))}
    skills = {
        "javascript": ensure_skill("JavaScript", ["JS", "ECMAScript"]),
        "python": ensure_skill("Python", ["py"]),
        "sql": ensure_skill("SQL", ["structured query language"]),
        "react": ensure_skill("React", ["ReactJS"]),
    }
    log(f"categories/levels/skills ready: {len(skills)} skills")

    cc, cc2 = creators["qa-creator"], creators["qa-creator2"]
    C = {}
    C["js"] = ensure_course("qa-creator", cc, "JavaScript Fundamentals",
                            "Learn JavaScript from scratch: variables, functions, the DOM.",
                            [cat_prog, cat_web], lvl["Beginner"])
    C["js_adv"] = ensure_course("qa-creator", cc, "Advanced JavaScript Patterns",
                                "Closures, async patterns and modules for experienced JavaScript developers.",
                                [cat_web], lvl["Advanced"])
    C["python"] = ensure_course("qa-creator", cc, "Python for Data Analysis",
                                "Python, pandas and notebooks for analysing real data.", [cat_data], lvl["Beginner"])
    C["sql"] = ensure_course("qa-creator", cc, "SQL Essentials",
                             "Query relational databases with SQL: joins, grouping and indexes.",
                             [cat_data], lvl["Beginner"])
    C["web"] = ensure_course("qa-creator", cc, "Web Accessibility Basics",
                             "Build inclusive web pages: semantics, contrast and screen readers.",
                             [cat_web], lvl["Intermediate"])
    C["trading"] = ensure_course("qa-creator", cc, "Algorithmic Trading with Python",
                                 "Adults only: backtesting trading strategies in Python.", [cat_data],
                                 lvl["Intermediate"], age_lower=18)
    C["kotlin"] = ensure_course("qa-creator2", cc2, "Kotlin for Android",
                                "Kotlin language basics for Android apps.", [cat_prog], lvl["Beginner"])
    C["draft"] = ensure_course("qa-creator", cc, "Rust Systems Draft",
                               "Unpublished draft about Rust ownership and borrowing.", [cat_prog], lvl["Advanced"])
    C["archived"] = ensure_course("qa-creator", cc, "Legacy jQuery Widgets",
                                  "Archived course about jQuery plugins.", [cat_web], lvl["Beginner"])

    # Prerequisite A -> B must be set while B is still a draft; on a live course it would go to review.
    ensure_prerequisites("qa-creator", C["js_adv"], [C["js"]]) if course_row("Advanced JavaScript Patterns")[2] != "t" else None
    for key, name in (("js", "JavaScript Fundamentals"), ("js_adv", "Advanced JavaScript Patterns"),
                      ("python", "Python for Data Analysis"), ("sql", "SQL Essentials"),
                      ("web", "Web Accessibility Basics"), ("trading", "Algorithmic Trading with Python"),
                      ("kotlin", "Kotlin for Android"), ("archived", "Legacy jQuery Widgets")):
        publish_and_approve("qa-creator2" if key == "kotlin" else "qa-creator", C[key], name)
    if course_row("Legacy jQuery Widgets")[1] != "archived":
        must(call("POST", f"/api/v1/courses/{C['archived']}/archive", "qa-creator"), "archive jQuery")

    ensure_course_skills("qa-creator", C["js"], [(skills["javascript"], "beginner", 5)])
    ensure_course_skills("qa-creator", C["js_adv"], [(skills["javascript"], "advanced", 5),
                                                     (skills["react"], "intermediate", 2)])
    ensure_course_skills("qa-creator", C["python"], [(skills["python"], "beginner", 5), (skills["sql"], "beginner", 2)])
    ensure_course_skills("qa-creator", C["sql"], [(skills["sql"], "beginner", 5)])
    ensure_course_skills("qa-creator", C["trading"], [(skills["python"], "intermediate", 4)])

    # A published course with an edit waiting for review: updating a live, approved course writes a shadow draft.
    if not sql_value(f"SELECT uuid FROM courses WHERE parent_course_uuid = {q(C['web'])}"):
        live = must(call("GET", f"/api/v1/courses/{C['web']}", "qa-creator"), "read web course")
        live = {k: v for k, v in live.items() if v is not None}
        live["description"] = "PENDING EDIT: semantics, contrast, screen readers and ARIA live regions."
        must(call("PUT", f"/api/v1/courses/{C['web']}", "qa-creator", live), "shadow edit web course")
    log(f"courses: {C}")

    R = {
        "public": ensure_rubric("qa-creator", cc, "Public Project Rubric", True),
        "private": ensure_rubric("qa-creator", cc, "Private Chloe Rubric", False),
        "private_other": ensure_rubric("qa-creator2", cc2, "Private Carl Rubric", False),
    }
    log(f"rubrics: {R}")

    inst, inst2 = instructors["qa-instructor"], instructors["qa-instructor2"]
    for key in ("js", "python", "sql", "web"):
        ensure_training_approval(C[key], "qa-instructor", inst)
        ensure_training_approval(C[key], "qa-orgadmin", org, applicant_type="organisation")
    ensure_training_approval(C["python"], "qa-instructor2", inst2)
    ensure_training_approval(C["python"], "qa-creator2", org2, applicant_type="organisation")
    # Instructor skills resolve to the taxonomy by name/alias (JS -> JavaScript).
    for name, level in (("JS", "ADVANCED"), ("Python", "INTERMEDIATE")):
        if not sql_value(f"SELECT uuid FROM instructor_skills WHERE instructor_uuid = {q(inst)} "
                         f"AND skill_name = {q(name)}"):
            must(call("POST", f"/api/v1/instructors/{inst}/skills", "qa-instructor", {
                "instructor_uuid": inst, "skill_name": name, "proficiency_level": level}), f"instructor skill {name}")
    log("training approvals and instructor skills ready")

    K = {
        "public_nairobi": ensure_class("qa-orgadmin", "JavaScript Bootcamp Nairobi", C["js"], org, branch, inst,
                                       "PUBLIC", "IN_PERSON", NAIROBI_CLASS, 7, 6),
        "private": ensure_class("qa-orgadmin", "SQL Private Cohort", C["sql"], org, branch, inst,
                                "PRIVATE", "IN_PERSON", NAIROBI_CLASS, 8, 6),
        "online": ensure_class("qa-orgadmin", "Python Online Evenings", C["python"], org, branch, inst,
                               "PUBLIC", "ONLINE", None, 9, 15),
    }
    log(f"classes: {K}")

    branch2 = ensure_branch(org2, "Mombasa Island", (-4.0435, 39.6682), "qa-creator2")
    J = {
        "open": ensure_job("qa-orgadmin", "JavaScript Weekend Trainer", org, branch, C["js"], "IN_PERSON",
                           NAIROBI_BRANCH, 14, 7),
        "filled": ensure_job("qa-orgadmin", "SQL Evening Trainer", org, branch, C["sql"], "IN_PERSON",
                             NAIROBI_BRANCH, 15, 15),
        "other_org": ensure_job("qa-creator2", "Python Mombasa Trainer", org2, branch2, C["python"], "ONLINE",
                                None, 16, 7),
    }
    ensure_job_required_skills("qa-orgadmin", J["open"], [(skills["javascript"], "intermediate", True),
                                                          (skills["react"], "beginner", False)])
    # FILLED is reached through a hire; the seed sets the end state directly (no applicant flow needed here).
    sql(f"UPDATE class_marketplace_jobs SET status = 'FILLED' WHERE uuid = {q(J['filled'])} AND status <> 'FILLED'")
    log(f"jobs: {J}")

    # qa-student enrols through the API (which also affiliates them with the organisation and branch).
    ensure_class_enrolment("qa-student", students["qa-student"], K["public_nairobi"], C["js"])
    ensure_review("qa-student", students["qa-student"], C["js"], 5, "Clear and practical")
    ensure_skill_goals("qa-student", students["qa-student"], [skills["python"], skills["sql"]])
    # Co-enrolment: 6 adults share JavaScript Fundamentals + Python for Data Analysis (k=6 >= 5: stored).
    adults = ensure_synthetic_learners("adult", 8, "1994-01-01")
    for i, s in enumerate(adults[:6]):
        ensure_course_enrolment_sql(s, C["js"], "completed" if i < 4 else "active", days_ago=20 + i)
        ensure_course_enrolment_sql(s, C["python"], "active", days_ago=5 + i)
    # ...and SQL Essentials + Web Accessibility Basics with the minor among 7 learners (k=7 < 10: never stored).
    for i, s in enumerate(adults[2:8]):
        ensure_course_enrolment_sql(s, C["sql"], "completed", days_ago=30 + i)
        ensure_course_enrolment_sql(s, C["web"], "active", days_ago=8 + i)
    ensure_course_enrolment_sql(students["qa-minor"], C["sql"], "completed", days_ago=25)
    ensure_course_enrolment_sql(students["qa-minor"], C["web"], "active", days_ago=6)
    ensure_guardian_link(students["qa-minor"], users["qa-guardian"])
    ensure_skill_goals("qa-minor", students["qa-minor"], [skills["python"]])
    log("enrolments, reviews, guardian link and skill goals ready")

    refresh_features()
    rebuild_indexes_and_wait()
    log(f"done in {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main()
