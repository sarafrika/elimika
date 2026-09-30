# Job search and matching (rules-v1)

Two read-only endpoints rank marketplace jobs and instructors against each other. **Hard eligibility is
unchanged and always decides**: matching only orders and explains. A job the instructor view reports as
eligible is exactly one `POST /api/v1/classes/jobs/{jobUuid}/applications` accepts, because both use the
job service's own eligibility path.

- Instructor pay never enters any index; it is read from the hydrated rows.
- Organisations see a fit summary only: `score`, `reasons`, `schedule_clear`, `rate_within_budget`. Never the
  instructor's rate, the clashing sessions, how many clash, or the diary.
- Distance is used only for instructors who opted in to location search (`location_search_opt_in`), and
  only ever as a band ("About 2-5 km away"), never metres.

## Flow

```
 UI                                  API                                         storage
 ──                                  ───                                         ───────
 Instructor "Matched jobs" ─ GET /api/v1/classes/jobs/matches?limit=&radius_km= ─► JobMatchService (classes)
                             (instructor only)                   1. course-spi findInstructorApprovals ─► course_training_applications,
                                                                                                         program_training_applications
                                                                 2. marketplace_jobs index: OPEN, course_uuid IN approved
                                                                    OR program_uuid IN approved, registration not closed,
                                                                    _geoRadius only if radius_km AND opted in (≤100 hits)
                                                                 3. hydrate ─► class_marketplace_jobs (pay), required skills
                                                                    (own tags or course_skills), instructor-spi profile
                                                                    (instructor_skills, experience, reviews, opted-in point)
                                                                 4. score, then batch eligibility on the top 50
                                                                    (verified, approved, rate covered, no clash, no live application)
                                                                 5. ineligible last ─► discovery_events (surface job_matches)

 Organisation job screen ─ GET /api/v1/classes/jobs/{jobUuid}/candidates?limit= ─► JobMatchService
                           (org managers + platform admins;      1. course-spi approvedInstructorUuidsForCourse/Program
                            403 otherwise)                       2. instructors index (instructor-spi): uuid IN approved AND
                                                                    admin_verified; IN_PERSON job with a point: _geoRadius 25 km
                                                                    first, then a query without geo (never-opted-in instructors
                                                                    stay in, located neutrally); merged, ≤100
                                                                 3. SQL re-check: verified AND approved; score; rates for the
                                                                    top 40; reverse eligibility (job × instructors) on the top 20
                                                                 4. fit summary only ─► discovery_events (surface job_candidates)
```

## Scoring (`JobMatchScoring`, model `rules-v1`)

```
score = 0.35·skill_coverage + 0.20·location_fit + 0.15·rate_fit + 0.10·experience + 0.10·rating + 0.10·urgency
        × 0.3 when a mandatory required skill is missing
        + 0.1 when the instructor is the job's preferred_instructor_uuid
        clipped to 0..1, 4 decimal places
```

| Component | Rule |
|---|---|
| `skill_coverage` | required skills the instructor holds at or above `min_proficiency` ÷ required skills. No required skills: 1.0, and no "Matches X/Y" reason. Only instructor skills linked to the taxonomy (`instructor_skills.skill_uuid`) count |
| `location_fit` | ONLINE job: 1.0. Otherwise by band between the opted-in instructor's point and the job's point: <2 km 1.0, 2-5 0.9, 5-10 0.75, 10-25 0.5, >25 0.2. Not opted in, or no job point: 0.5 (neutral) |
| `rate_fit` | `clip((pay − approved_rate) / approved_rate, 0, 0.5) × 2`; 0 without a rate. Pay covering the rate is a hard eligibility rule |
| `experience` | `min(years / 5, 1)`; years summed from `instructor_experience` (stated years, else the dates) |
| `rating` | Bayesian average of `instructor_reviews` (prior: platform mean weighted as 5 reviews) ÷ 5; 0.5 before the platform has reviews |
| `urgency` | 1.0 when registration closes within 3 days, falling linearly to 0 at 30 days; 0.25 without a closing date |

Reasons (plain text; each has a machine code recorded with the impression):

| Code | Instructor view | Organisation view |
|---|---|---|
| `PREFERRED` | "The organisation asked for you" | "Your preferred instructor" |
| `SKILLS_MATCHED` | "Matches 4/5 required skills" | same |
| `APPROVED` | "Approved to teach {course or program}" | same |
| `NEARBY` | "About 2-5 km away" (opted-in only) | same |
| `PAY_ABOVE_RATE` | "Pay is 20% above your approved rate" | never |
| `SCHEDULE_CLEAR` | "No schedule clashes" | same |
| `CLOSING_SOON` | "Registration closes in 3 days" (≤7 days) | - |
| `EXPERIENCED` | - | "4 years of experience" |
| `RATED` | - | "Rated 4.6/5 from 12 reviews" |

## API

### `GET /api/v1/classes/jobs/matches?limit=20&radius_km=`

Instructors only (403 for anyone else). `limit` 1-50 (default 20). `radius_km` 2-100, ignored unless the
instructor opted in to location search and has coordinates. 503 when search is unavailable.

```json
{ "recommendation_id": "…", "model_version": "rules-v1",
  "items": [ { "uuid": "…", "title": "…", "instructor_pay": 240.00, "…": "every ClassMarketplaceJob field",
               "match": { "score": 0.8125,
                          "matched_skills": [ { "skill_uuid": "…", "skill_name": "Python", "min_proficiency": "intermediate", "is_mandatory": true } ],
                          "required_skills": [ … ],
                          "reasons": [ "Matches 1/2 required skills", "Approved to teach Data Analysis", "No schedule clashes" ],
                          "eligibility": { "eligible": true, "rate_ok": true, "schedule_clear": true, "reason": null, "…": "as /eligibility" } } } ] }
```

### `GET /api/v1/classes/jobs/{jobUuid}/candidates?limit=20`

Managers of the posting organisation and platform admins (the `listJobApplications` rule); 403 otherwise.
`limit` 1-20 (default 20). Candidates whose schedule is clear and whose rate the pay covers come first.

```json
{ "recommendation_id": "…", "model_version": "rules-v1", "job_uuid": "…",
  "items": [ { "instructor_uuid": "…", "display_name": "Jane Doe", "location_name": "Nairobi", "admin_verified": true,
               "match": { "score": 0.74, "reasons": [ "Matches 2/3 required skills", "No schedule clashes" ],
                          "schedule_clear": true, "rate_within_budget": true } } ] }
```

Clicks and dismissals go to `POST /api/v1/discovery/events` with the `recommendation_id`
(item types `marketplace_job` and `instructor`).

## Module surface

- `course :: course-spi` — `CourseTrainingApprovalSpi.approvedInstructorUuidsForCourse(courseUuid)` and
  `approvedInstructorUuidsForProgram(programUuid)`.
- `instructor :: instructor-spi` — `InstructorMatchingService.findMatchProfiles(uuids)` (skills by taxonomy
  UUID, years, rating, opted-in point) and `searchVerifiedAmong(uuids, near, limit)` over the `instructors`
  index; implemented in `instructor/search/InstructorMatchingServiceImpl`.
- `classes` — `classes/internal/matching/JobMatchService` and `JobMatchScoring`. The job service's
  eligibility was split into per-instructor facts plus one decision (`decideEligibility`) shared by the
  instructor's own eligibility reads, the matches and the new reverse check
  (`assessEligibilityForCandidates(job, instructorUuids)`), which expands the job's sessions once and
  reuses `findInstructorScheduleConflicts` per instructor.
