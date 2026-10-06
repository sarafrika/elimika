# Onboarding

## Intent

One onboarding wizard serves every domain (student, instructor, course creator, organisation, parent).
Each domain is a list of ordered steps with progress and, where the domain needs approval, a submit for
review. A user can hold several domains, and the account, professional profile and skills wallet belong to the
**user**, not the domain (see [user-profile-and-wallet.md](user-profile-and-wallet.md)). So an instructor who
adds the course creator domain finds those steps already done (`shared: true`, `complete: true`) and only has
the course creator's own steps left.

Registration and the approval states are described in [registration-and-approval.md](registration-and-approval.md).

## UI ↔ API ↔ Storage Flow

```text
Onboarding wizard (UI, signed in; works while the domain is pending approval)
  | GET  /api/v1/onboarding
  |   -> [{ domain, status, requires_approval, active, steps_completed, steps_total,
  |         ready_for_submission, submitted_at }]                     one row per domain held (admin excluded)
  | GET  /api/v1/onboarding/{domain}
  |   -> { domain, status, requested, requires_approval, active,
  |        steps: [{ key, title, required, complete, shared, missing: [...], counts: {...} }],
  |        steps_completed, steps_total, ready_for_submission, submitted_at, reviewed_at, review_reason }
  |   a domain not requested yet returns a preview (requested = false, status not_started)
  | each step links to the screen that fills it (table below); the wizard re-reads the domain afterwards
  | POST /api/v1/onboarding/{domain}/submit
  |   409 required steps missing | already submitted | already approved;  404 domain never requested
  v
tenancy OnboardingService
  | resolves the domain profile (instructor / course creator / student uuid)
  | asks every OnboardingStepProvider (shared.spi.onboarding) that supports the domain, merges by position:
  |   tenancy     account, organisation, organisation_documents
  |   profile     professional_profile, skills_wallet
  |   instructor  teaching_location        availability  availability
  |   coursecreator categories             student       student_profile, guardians
  | status from user_domain_mapping: APPROVED -> approved, REJECTED -> rejected, SUSPENDED -> suspended,
  |   PENDING + submitted_at -> submitted, PENDING with a non-account step done -> in_progress, else not_started
  | submit (one transaction):
  |   providers' onSubmitted   course_creators.verification_status = SUBMITTED (+ submitted_at)
  |                            organisation.verification_requested_at for unverified organisations the user runs
  |   DomainApprovalService.recordSubmission
  |     approval domains: user_domain_mapping.status = PENDING, submitted_at = now, review fields cleared
  |                       (a rejected or suspended domain can be resubmitted) -> admins notified
  |                       (DOMAIN_APPROVAL_REQUESTED, "Application ready for review", once per submission)
  |     student / parent: submitted_at stamped once as "onboarding finished"; the domain stays active
  v
Platform admin (UI admin queue)
  | GET  /api/v1/admin/registrations?status=PENDING&domain=&submitted=true|false
  |   -> [{ user_uuid, full_name, email, domain, status, requested_at, submitted_at, reviewed_at, review_reason }]
  | POST /api/v1/admin/users/{userUuid}/domains/instructor/moderate?action=approve|reject|revoke
  |   -> mapping decided, DomainModeratedEvent -> instructors.admin_verified follows (approve true, else false)
  | POST /api/v1/admin/instructors/{uuid}/verify          -> also approves the instructor domain
  | POST /api/v1/admin/course-creators/{uuid}/moderate    -> course creator review + domain
  | POST /api/v1/admin/organizations/{uuid}/moderate      -> organisation + its admins' organisation_user domain
  v
user_domain_mapping.status APPROVED -> user notified, onboarding status approved, active = true
```

## Steps per Domain

| Domain | Step (`key`) | Required | Shared | Complete when | Filled through |
|---|---|---|---|---|---|
| all | `account` | yes | yes | first name, last name, email and phone on the user | `PUT /api/v1/users/{uuid}` (Keycloak) |
| instructor, course_creator | `professional_profile` | yes | yes | headline and bio | `PUT /api/v1/me/profile` |
| instructor, course_creator | `skills_wallet` | yes | yes | at least one skill; `counts` holds items per wallet section | `/api/v1/me/profile/{section}` |
| instructor | `teaching_location` | yes | yes | `location_name` on the profile basics | `PUT /api/v1/me/profile` |
| instructor | `availability` | no | no | at least one open availability slot (`counts.slots`) | `/api/v1/instructors/{uuid}/availability` |
| course_creator | `categories` | yes | no | at least one category preference (`counts.categories`) | `PUT /api/v1/course-creators/me/onboarding/categories` |
| student | `student_profile` | yes | no | the student row exists | `POST /api/v1/students` |
| student | `guardians` | under 18 only | no | a guardian named or linked, or the learner is an adult; optional when the age is unknown | `guardians` on `POST/PUT /api/v1/students` |
| organisation_user | `organisation` | yes | no | an organisation the user administers has a name and a country or location | `POST/PUT /api/v1/organisations` |
| organisation_user | `organisation_documents` | yes | no | every required `ORGANISATION` document type uploaded and not rejected (`counts.uploaded/required`) | `POST /api/v1/organisations/{uuid}/documents/upload` |
| parent | `account` only | | | | |

`missing` names what is outstanding (`phone_number`, `bio`, `skills`, `categories`, `guardian`, `country`,
`CERTIFICATE_OF_REGISTRATION`, ...).

## Multi-Domain Reuse

```text
User (one account, one professional profile, one wallet)
  |-- instructor      account ✓  professional_profile ✓  skills_wallet ✓  teaching_location ✓  availability ·
  |-- course_creator  account ✓  professional_profile ✓  skills_wallet ✓  categories ·
  '-- student         account ✓  student_profile ✓  guardians ·
     ✓ written once, read by every domain (shared = true)      · owned by the domain
```

Submitting is per domain: submitting the instructor domain does not submit the course creator domain, and an
admin decides each one on its own.

## Storage

`V202610061327__add_submitted_at_to_user_domain_mapping.sql` adds `user_domain_mapping.submitted_at` and
backfills it from `course_creators.submitted_at` (creators already past `DRAFT`) and from
`organisation.verification_requested_at` for the admins of organisations that already asked for verification.
Instructors pending before this change have no `submitted_at`; they appear under `submitted=false` until they
submit (the queue without the filter still lists them).

`POST /api/v1/organisations/{uuid}/request-verification` also stamps `submitted_at` on the pending
organisation_user domain of the organisation's admins.

## Legacy Course Creator Routes

`GET /api/v1/course-creators/me/onboarding` and `PUT .../categories` are unchanged.
`POST /api/v1/course-creators/me/onboarding/submit` now delegates to the generic submit (same rules and 409s)
and returns the old response shape. Both onboarding read and submit routes are marked deprecated for one release.
