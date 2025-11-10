# Guardian / Parent Dashboard Access

This guide explains how the guardian access APIs work and how the frontend should consume them so that parents can monitor their children with their own credentials.

## Backend Enforcement Recap
- Guardians are linked to students through `/api/v1/guardians/links` (admin/instructor only). Every link is stored in `student_guardian_links` and audited.
- Once a guardian is linked, the system automatically assigns them to the `parent` domain so their Keycloak identity can be recognized server-side.
- Guardians can view only the students returned by `GET /api/v1/guardians/me/students`. Any attempt to access another learner will fail the security check.
- `GET /api/v1/guardians/students/{studentUuid}/dashboard` returns a read-only snapshot with:
  ```json
  {
    "student_uuid": "…",
    "student_name": "…",
    "share_scope": "FULL|ACADEMICS|ATTENDANCE",
    "status": "ACTIVE|PENDING|REVOKED",
    "course_progress": [
      {
        "enrollment_uuid": "…",
        "course_uuid": "…",
        "course_name": "Intro to Algebra",
        "status": "ACTIVE",
        "progress_percentage": 72.5,
        "updated_date": "2025-02-04T11:30:00Z"
      }
    ],
    "program_progress": [
      {
        "enrollment_uuid": "…",
        "program_uuid": "…",
        "program_name": "STEM Accelerator",
        "status": "COMPLETED",
        "progress_percentage": 100.0,
        "updated_date": "2025-02-01T09:15:00Z"
      }
    ]
  }
  ```
- When a guardian’s share scope is `ATTENDANCE`, both progress arrays are intentionally empty—the frontend should hide academic widgets in that case.

## Frontend Workflow
1. **Session bootstrap**
   - After a guardian signs in, call `GET /api/v1/guardians/me/students`.
   - If the list is empty, show an onboarding banner explaining that an instructor must link them to a learner.
   - Otherwise, surface the first student by default and allow switching via a dropdown or tabs.

2. **Dashboard rendering**
   - Fetch dashboard data with `GET /api/v1/guardians/students/{uuid}/dashboard`.
   - Use `course_progress` data to render cards with course name, status badge, and progress bar.
   - Use `program_progress` to show higher-level aggregates (e.g., journey timeline).
   - Respect `share_scope`:
     - `FULL`: show academic + attendance + commerce actions.
     - `ACADEMICS`: hide billing/attendance widgets but keep coursework.
     - `ATTENDANCE`: only show attendance/announcements; hide the grade/progress sections since arrays arrive empty.

3. **Error handling**
   - `403` means the guardian tried to view an unlinked learner—prompt them to contact support.
   - `404` indicates the student UUID is invalid or link was revoked—refresh the student list and redirect.

4. **UI cues**
   - Surface the guardian relationship (Parent/Guardian/Sponsor) from `GET /me/students` for clarity.
   - When a link status is `PENDING`, show a badge that the child must accept the invitation; hide dashboard shortcuts until status becomes `ACTIVE`.

5. **Polling/refresh**
   - For long-running study sessions, poll the dashboard endpoint every ~60 seconds or expose a manual refresh button; the payload is lightweight and read-only.

By following this flow, the frontend stays aligned with the backend authorization rules while giving guardians a clear, scoped view of their learner’s progress.
