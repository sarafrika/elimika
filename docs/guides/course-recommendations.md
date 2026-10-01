# Course recommendations (rules-v2)

Personal course recommendations built from real relationships in the data: the learner's enrolments and
progress, prerequisites, difficulty levels, co-enrolment, categories, declared skill goals and the
organisations and instructors they learn with. **SQL decides, Meilisearch narrows, Java ranks.** Every item
explains itself with `reasons[]`, and no reason ever names another learner.

This replaces the prototype, which never read a learner's enrolments (students always got the fallback) and
whose "popular" list was really "newest".

## Flow

```
 UI                                   API                                               storage
 ──                                   ───                                               ───────
 Home / dashboard
   GET /api/v1/courses/recommendations?surface=for_you|next_steps&student_uuid=&limit=
        │
        ▼
 CourseController (shared) ──► CourseRecommendationServiceImpl (course)
                                  │ access: self | platform admin | guardian with FULL/ACADEMICS share
                                  │         (LearnerProfileLookupService, student module)
                                  │ 1 profile ── LearnerContextLoader
                                  │     course_enrollments ⨝ courses, categories, skills, levels ─ 1 query
                                  │     skill goals, age (a number; dob stays in tenancy) ── student SPI
                                  │     affiliations ── LearnerAffiliationLookup (timetabling: class
                                  │                      enrolments + live org memberships)
                                  │     course_co_enrolments (k ≥ 5, k ≥ 10 with minors) ─ 1 query
                                  │     course_training_applications (approved offers) ─ 1 query
                                  │ 2 candidates ── CourseCandidateRetriever
                                  │     search on:  one POST /multi-search over `courses`
                                  │                 scope: is_public AND age band; filter: NOT uuid IN enrolled
                                  │                 sets: categories (rating_bayes desc) | co-neighbours |
                                  │                       affiliated offers | skill gap | follow-ons of own
                                  │                       courses | "more like this" (matchingStrategy LAST) |
                                  │                       popular (popularity_30d desc)
                                  │     search off: the same set filters as one SQL query
                                  │     either way: hydrate the union through the public-catalogue SQL
                                  │                 (drops stale hits, re-applies age band and exclusions)
                                  │ 3 RecommendationScorer ── weights, penalties, diversity, reasons
                                  │ 4 DiscoveryTracker.recordImpressions (surface, recommendation_id,
                                  │                                       model_version = rules-v2)
                                  ▼
 [{course_uuid, name, description, thumbnail_url, reason, score,
   reasons[{code, text, related_uuid}], recommendation_id, surface, model_version}]
        │
        └─ click / dismiss ──► POST /api/v1/discovery/events {recommendation_id, item_uuid, ...}

 Course page
   GET /api/v1/courses/{uuid}                    (anonymous allowed; public courses only, no revenue terms)
   GET /api/v1/courses/{uuid}/prerequisites|skills  (anonymous allowed; public courses only)
        ──► CourseService.getVisibleCourseByUuid ──► courses (404 for anything not public when anonymous)
   GET /api/v1/courses/{uuid}/similar?limit=     (anonymous allowed; public courses only)
        ──► co-neighbours of the course + same categories + "more like this" ──► same item shape,
            surface = similar; impressions only for signed-in callers

 Learner profile
   GET/PUT /api/v1/students/{uuid}/skill-goals ──► LearnerSkillGoalService (student) ──► learner_skill_goals
```

## API

### `GET /api/v1/courses/recommendations`

| Param | Meaning |
|---|---|
| `surface` | `for_you` (default) or `next_steps`; anything else is 400 |
| `student_uuid` | A learner's list. Allowed for the learner, a platform admin, or a guardian with an active FULL or ACADEMICS share; otherwise 403 |
| `user_uuid` | Defaults to the caller; a non-admin naming another user gets 403. Not combinable with `student_uuid` (400) |
| `limit` | 1-50, default 6 |

Response items keep the old fields (`course_uuid`, `name`, `description`, `thumbnail_url`, `reason`, `score`)
and add `reasons[]`, `recommendation_id` (one per response), `surface` and `model_version` (`rules-v2`).
`reason` is `reasons[0].text`.

Excluded always: courses the learner has any enrolment in (any status), courses that are not public
(published, admin-approved, active, not a shadow draft), and courses outside the learner's age band. The age
is computed at request time by tenancy and only the number crosses modules; a learner **without a date of
birth** sees only courses with no age limit (enrolling in a limited one needs a date of birth anyway).

- **for_you**: the ranked list. Courses with an unmet mandatory prerequisite are left out.
- **next_steps**: courses that follow on from the learner's own: a completed prerequisite, one difficulty
  level above their best completed level in the category, or a course whose mandatory prerequisite they are
  still taking ("Complete X first"). Empty for a learner with no enrolments.
- **No history, goals or affiliations**: the most enrolled courses of the last 30 days in the learner's age
  band, each with a `POPULAR` reason.

### `GET /api/v1/courses/{uuid}/similar?limit=`

Anonymous-capable (`permitAll` in `SecurityConfiguration`); 404 when the course is not public. Not personal:
co-enrolment neighbours, shared categories and "more like this" on the course name. No age band.

The page an anonymous visitor lands on from a similar course is anonymous-capable too:
`GET /api/v1/courses/{uuid}`, `/prerequisites` and `/skills` answer a caller without a token for **public**
courses only (root, published, admin-approved, active) and 404 for drafts, shadow drafts, unapproved and
archived courses. The anonymous course record leaves out `minimum_training_fee`,
`creator_share_percentage`, `instructor_share_percentage`, `revenue_share_notes`, `created_by` and
`updated_by`. Signed-in callers keep the existing rules. The `permitAll` matcher is a UUID regex, so
`/courses/search`, `/courses/active` and the other listings stay authenticated.

### `GET /api/v1/admin/recommendations/evaluation` (platform admin)

Offline leave-last-out over `course_enrollments`: each learner with 2+ enrolments whose latest one is a public
course has it hidden; features (co-enrolment lifts with the same k thresholds, popularity) are rebuilt from
the earlier rows only. Reports `recall_at_k`, `ndcg_at_k` and `coverage` (k = 6) for `rules-v2`, a
`popularity` baseline and `legacy-newest` (what the old engine gave students). Skill goals and affiliations
are left out because they are not time-sliced. A regression gate, not a tuning target.

### `GET|PUT /api/v1/students/{uuid}/skill-goals`

`PUT` (the learner only) takes `{"skill_uuids": [...]}` (at most 20; `[]` clears) and returns
`[{skill_uuid, name, slug, source, created_date}]`. Unknown skills, duplicates and newly added retired skills
are 400. `GET` is open to the learner, a platform admin and a guardian with a FULL or ACADEMICS share.

## Scoring (roadmap §10)

```
score = 3.0·next_step + 2.5·co_enrol + 2.0·category_affinity + 3.0·skill_gap
      + 1.5·affiliation + 1.0·quality + 0.5·popularity
```

| Term | Value |
|---|---|
| `next_step` | 1 when a prerequisite is completed, or the course is exactly one level above the best completed level in a shared category |
| `co_enrol` | max over the learner's non-dropped courses of `weight · lift / (1 + lift)`; weight 1 when completed, else progress (at least 0.25) |
| `category_affinity` | completion-weighted category share, normalised to the learner's top category; parent or child categories count half |
| `skill_gap` | share of goal skills (minus those taught by completed courses) the course teaches |
| `affiliation` | 1 when an affiliated organisation or instructor is approved to train the course |
| `quality` | `rating_bayes / 5` (0.5 before any review exists) |
| `popularity` | `log1p(enrolments_30d) / log1p(max among candidates)` |

Penalties: more than one level above the reference level ×0.5; a category the learner mostly dropped ×0.6.
Diversity: at most 2 per category in the first 6, the 6th slot reserved for the best course outside all the
learner's categories (exploration); if the cap leaves the window short it is filled in score order.

| Code | Text |
|---|---|
| `NEXT_STEP` | Next step after {course} |
| `PREREQUISITE_PENDING` | Complete {course} first (next_steps only) |
| `CO_ENROLLED` | Often taken after {course} (similar: Often taken with {course}); only pairs stored by the nightly job, which enforces k ≥ 5 / k ≥ 10 with minors, re-checked on read |
| `CATEGORY` | Because you completed / you're taking {course} in {category} (similar: Also in {category}) |
| `SKILL_GAP` | Teaches {n} of {m} skills you want to learn |
| `AFFILIATION` | Offered by {organisation or instructor} |
| `SIMILAR_CONTENT` | Covers similar topics to {course} (similar only) |
| `POPULAR` | Popular in {category}, measured by real 30-day enrolments |

## Storage

| Table | Owner | Notes |
|---|---|---|
| `learner_skill_goals` | student | `student_uuid` (FK, cascade), `skill_uuid` (FK, cascade), `source` (`SELF`/`GUARDIAN`/`ADMIN`), audit; UNIQUE (student, skill) |
| `course_learning_stats`, `course_co_enrolments`, `course_prerequisites`, `course_skills` | course | Read only; written by `CourseFeatureRefreshJob` and the course editor |

## Cost per request

Course-module SQL is a fixed handful of statements whatever the number of enrolments or candidates
(enrolments, co-neighbours, approved offers, candidates), plus the student/timetabling SPI lookups and one
search call. The integration test asserts the count does not grow.
