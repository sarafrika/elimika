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

### Enrollment Compliance Notifications
- If the enrollment API rejects a request with `AgeRestrictionException`, capture the payload and surface a banner in the guardian dashboard showing the course/class name, allowed age range, and guidance to pick an age-appropriate alternative.
- Prompt guardians to confirm the learner’s DOB when a rejection occurs; wire the CTA to the profile editor so they can fix typos without contacting support.
- Provide quick links to filtered course search (pre-populated with the guardian’s learner age) so families can immediately retry with offerings that pass the age gate.

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

## Recommended Layout & Controls
### Global shell
- **Header:** show guardian name, “Parent Portal” label, and the active student selector (pill, dropdown, or segmented control). Include a `Link another learner` CTA only for admins/instructors.
- **Left rail (desktop):** quick links for Dashboard, Attendance, Notifications, and Billing (if share scope allows). Collapse to a bottom nav on mobile.
- **Primary content stack:** use cards with consistent spacing and color-coded status chips (e.g., success for `COMPLETED`, warning for `PENDING`, neutral for `ACTIVE`).

### Student selector
- Use the `/me/students` payload to build options with: avatar initials, student name, relationship badge, and status pill.
- Persist the last selected student in local storage so a guardian re-opens the same view after refresh.
- Disable or gray out entries whose `status` ≠ `ACTIVE`; clicking should open a modal explaining the pending/revoked state.

### Dashboard widgets
1. **Learner summary card**
   - Show name, grade level (if available), relationship, share scope badge, and last synced time.
   - Provide quick-action buttons: “View Attendance”, “Download Report Card” (only for `FULL` / `ACADEMICS`).

2. **Course progress grid**
   - Render up to 10 items from `course_progress`.
   - Each tile: course name, instructor chip (optional future enhancement), progress bar, mini timeline showing `updated_date`.
   - Clicking opens the existing student course details page but with guardian read-only mode.

3. **Program milestones**
   - Use timeline or stacked cards for `program_progress`: show program name, status, progress radial chart, and expected completion date (derive from enrollment metadata if needed).

4. **Attendance + communications**
   - For `ATTENDANCE` scope, make this section full width and hide other academic cards.
   - Include last 5 attendance entries or announcements fetched from existing endpoints; keep placeholders ready even if the backend payload is empty.

### Controls and states
- **Loading:** skeleton rows for course/program cards; spinner in student selector.
- **Empty states:** friendly illustrations plus CTA (“Ask the instructor to link you”) when no students exist; “No active courses yet” message when arrays are empty for `FULL/ACADEMICS`.
- **Errors:** toast + inline banner with retry button; on `403/404`, fallback to student picker view.
- **Filters:** optional chips to filter course cards by `status` (Active, Completed) using client-side filtering of the response.

## Monitoring & Accessibility Tips
- Announce student changes via ARIA live regions so screen readers know the dashboard content refreshed.
- Ensure status colors meet WCAG contrast and pair them with text labels, not color alone.
- Log guardian actions (student switch, refresh clicks) to help support teams audit usage if access disputes arise.

By following this flow, the frontend stays aligned with the backend authorization rules while giving guardians a clear, scoped view of their learner’s progress.
