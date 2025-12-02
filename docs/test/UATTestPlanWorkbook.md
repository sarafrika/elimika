# Elimika UAT Test Workbook Guide

This guide describes how QA should build a multi‑sheet Excel workbook to cover UAT across every user domain and feature in Elimika. Each worksheet corresponds to a domain and lists granular scenarios, inputs, expected results, and verification notes. Use UTC timestamps in all date/time fields.

## Workbook Structure
- One worksheet per domain (listed below).
- Standard columns for every worksheet:
  - `ID` (e.g., AUTH-01)
  - `Feature`
  - `Scenario / Step`
  - `Test Data / Pre-conditions`
  - `API / UI Path`
  - `Expected Result`
  - `Artifacts to Capture` (screenshots, logs, payloads)
  - `Status` (Pass/Fail/Blocked/Not Run)
  - `Remarks` (defect ID, notes)
- Use filters and freeze the header row on every sheet.
- Keep a cover sheet that links to each domain sheet and records environment details (API base URL, tenant/org, test accounts, secrets location).

## Domain Worksheets and Coverage

### 1) Authentication & User Domain
Features: user registration, login, logout, password reset, MFA (if configured), session expiry, roles/authorities propagation.
- Scenarios:
  - AUTH-01: Register new user (happy path) → account created, verification email sent (if enabled).
  - AUTH-02: Login with valid creds → access token issued, profile retrieved.
  - AUTH-03: Login with invalid creds → 401/clear error, no token.
  - AUTH-04: Password reset flow → email link delivered, password updated, old token invalidated.
  - AUTH-05: Role/authority check (e.g., instructor vs student) → protected endpoint denies/allows.
  - AUTH-06: Session expiry/refresh token → expired access requires refresh; refresh rotates successfully.
  - AUTH-07: Account lockout/brute force handling (if configured) → throttle and clear messaging.
Artifacts: response bodies (without secrets), email samples, audit logs.

### 2) Organisation & Administration
Features: organisation creation, admin verification, domain/guardian links, invitations.
- Scenarios:
  - ORG-01: Create organisation → stored, audit fields set.
  - ORG-02: Admin verification toggle → status and audit updated.
  - ORG-03: Invitation flow (create, accept, decline, expire) → state transitions correct.
  - ORG-04: Guardian/parent domain link → child relationships visible in UI and API.

### 3) Course Creator Domain (Authoring)
Features: course creation, categories, difficulty, requirements, lessons, content uploads via StorageService.
- Scenarios:
  - CC-01: Create course with metadata (title, category, difficulty) → stored, visible in list.
  - CC-02: Add requirements/prerequisites → reflected in course detail.
  - CC-03: Add lessons → ordering preserved.
  - CC-04: Upload lesson media (PDF/video/audio/image) via `POST /api/v1/courses/{courseUuid}/lessons/{lessonUuid}/content/upload` → file stored, `file_url` returned, metadata saved.
  - CC-05: Tiptap image upload using same endpoint → HTML contains returned `file_url`.
  - CC-06: Add quizzes/assignments/resources to lessons → persisted and retrieved.
  - CC-07: Delete/update lesson content → version reflects changes.

### 4) Courses & Catalog
Features: course search, bundle handling (if enabled), rubrics, certificates templates.
- Scenarios:
  - CRS-01: Search course by name/category/difficulty → relevant results.
  - CRS-02: Course detail retrieval → requirements, lessons, media links present.
  - CRS-03: Certificate template creation/attach → certificate issuance uses template.
  - CRS-04: Rubric retrieval and scoring levels → correct mappings.

### 5) Instructor Domain
Features: profile (general/education/experience/memberships), documents upload, skills, rate cards (via training applications), availability, private bookings, reviews/ratings analytics.
- Scenarios:
  - INST-01: Create/update profile sections → persisted; loading states behave.
  - INST-02: Upload instructor documents (PDF) via `/api/v1/instructors/{uuid}/documents/upload` → file stored; metadata saved.
  - INST-03: Add skills with proficiency enum → saved without enum errors.
  - INST-04: Training application rate card fields → all four combinations saved.
  - INST-05: Availability creation → slots visible; conflicts prevented.
  - INST-06: Student books private slot via `/api/v1/instructors/{instructorUuid}/availability/book` → slot blocked; status/color set.
  - INST-07: Submit review (`POST /api/v1/instructors/{uuid}/reviews`) → stored, unique per enrollment.
  - INST-08: Fetch reviews list and summary (`/reviews`, `/reviews/summary`) → average and count match inserted data.
  - INST-09: Analytics helper (average_rating, review_count) → matches raw reviews.

### 6) Classes (Definitions) & Scheduling
Features: class definition, location (Mapbox-ready), recurrence patterns, schedule generation, invite link (public enrollment), class-level enrollment listing.
- Scenarios:
  - CLS-01: Create class definition with date-time defaults, location fields for IN_PERSON/HYBRID → validation enforces name/lat/long.
  - CLS-02: ONLINE class without location → accepted.
  - CLS-03: Recurrence pattern creation and link to class → stored.
  - CLS-04: Generate schedule → instances created with denormalized fields.
  - CLS-05: List enrollments for class definition `GET /api/v1/classes/{uuid}/enrollments` → returns students across instances.
  - CLS-06: Invite link (public enrollment page) generation if available → link accessible without token; enrolment possible.

### 7) Timetabling (Scheduled Instances & Enrollments)
Features: schedule instance, enrollment, attendance, conflicts.
- Scenarios:
  - TT-01: Schedule instance → uses class defaults; status SCHEDULED.
  - TT-02: Enroll a student (course/class) → capacity enforced; waitlist if enabled.
  - TT-03: Get enrollments for instance → list matches inserts.
  - TT-04: Mark attendance → status changes to ATTENDED/ABSENT; timestamp set.
  - TT-05: Cancel instance → enrollments cancelled; reason stored.
  - TT-06: Conflict detection for instructor and student → overlapping times rejected.

### 8) Commerce & Payments
Features: purchases/orders, catalogue pricing, rate card alignment, paywall before enrollment, platform fees.
- Scenarios:
  - COM-01: Create order for class enrollment → totals match rate card and platform fee breakdown.
  - COM-02: Paywall enforced before enrollment when required → API blocks unpaid enrollment.
  - COM-03: Platform fee rule applied correctly → amounts and labels returned.
  - COM-04: Refund/cancellation flow (if supported) → status updated; fee handling correct.

### 9) Student Domain & Learning Progress
Features: student profile, enrollments, lesson progress, content/quiz/assignment attempts, reviews of instructors.
- Scenarios:
  - STU-01: Student profile create/update → persists.
  - STU-02: Enroll in class via public invite → success; enrollment visible in schedule.
  - STU-03: Lesson progress tracking → completion increments; percentage calculated.
  - STU-04: Quiz attempt submission → scored; stored responses.
  - STU-05: Assignment submission → upload stored; status reflected.
  - STU-06: Submit instructor review → linked to enrollment; visible in instructor reviews.

### 10) Availability (Instructor Scheduling)
Features: availability patterns, blocking slots, private bookings (student → instructor), conflict with classes.
- Scenarios:
  - AV-01: Create availability block → visible; overlapping blocks prevented.
  - AV-02: Block time for private booking (book endpoint) → converted to blocked slot.
  - AV-03: Conflict check with class schedules → booking rejected if overlaps scheduled class.
  - AV-04: Color code and status stored for blocked slots.

### 11) Notifications
Features: notification templates, delivery log, user notification preferences.
- Scenarios:
  - NOTIF-01: Template creation/update → correct rendering variables.
  - NOTIF-02: Send event-triggered notification (e.g., enrollment) → delivery log written.
  - NOTIF-03: Preferences respected (opt-out) → no notification sent; log indicates skip.

### 12) Documents & Storage
Features: StorageService uploads for course materials, certificates, profile documents, instructor docs; content retrieval.
- Scenarios:
  - DOC-01: Upload course material (PDF/video/audio/image) → stored under course_materials path.
  - DOC-02: Upload certificate PDF → stored; URL saved on certificate.
  - DOC-03: Instructor profile documents upload → stored under profile_documents; metadata persisted.
  - DOC-04: Access retrieved URLs → return 200 and correct content type (where permitted).

### 13) Assessments & Grades
Features: quizzes, assignments, rubrics, scoring levels, attempts, submissions.
- Scenarios:
  - ASM-01: Create quiz template questions/options → stored.
  - ASM-02: Student quiz attempt → answers saved; scoring respects rubric/grading levels.
  - ASM-03: Assignment creation and submission → upload accepted; status transitions.
  - ASM-04: Rubric scoring table applied → weights and levels honored.

### 14) Certificates
Features: template management, issuance, upload of certificate PDFs.
- Scenarios:
  - CERT-01: Create template and issue certificate → student can download.
  - CERT-02: Upload certificate PDF via `/api/v1/certificates/{uuid}/upload` → URL saved; retrieval works.

### 15) Guardian/Parent Domain (if enabled)
Features: guardian links to students, permissions on schedule/enrollment.
- Scenarios:
  - GDN-01: Link guardian to student → visible in student profile.
  - GDN-02: Guardian views student schedule → authorized view only.
  - GDN-03: Guardian receives notifications (if configured) → delivery logged.

### 16) Analytics & Reporting
Features: instructor rating summary, schedule counts, commerce metrics, demographics tags.
- Scenarios:
  - ANA-01: Instructor rating summary endpoint → matches manual aggregate.
  - ANA-02: Enrollment counts per status/time window → matches DB.
  - ANA-03: Class/schedule counts per period → matches DB.
  - ANA-04: Demographic tag effects (if used) → tags propagate to reports.

## How to Populate the Workbook
- Pull real test data for UUIDs (instructor, student, course, class definition, scheduled instance).
- For media uploads, note file names, sizes, and mime types.
- Capture both UI behavior (loading states, toasts) and API responses (status codes, JSON).
- Mark timezone assumptions: all back-end timestamps are UTC; front-end display may localize.
- For negative tests, record exact error messages and codes.

## Expected Result Guidance
- Always specify both API and UI expectations (e.g., “HTTP 201; toast ‘Enrollment created’; record visible in table”).
- Enumerate state changes: DB row created/updated, status transitions, audit fields set.
- Where aggregates are shown (ratings, counts), give the numeric expectation based on seeded data.

## Defect Tracking
- Use the `Remarks` column to link to defect IDs.
- If a scenario is Blocked (e.g., missing data, environment issue), note the blocker clearly.
