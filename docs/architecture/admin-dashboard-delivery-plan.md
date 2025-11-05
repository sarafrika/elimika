# Admin Dashboard Delivery Plan

This plan captures the outstanding engineering work required to unlock the full Admin Dashboard experience. All tasks reference concrete endpoints defined (or about to be added) in the Elimika Spring Boot service.

---

## 1. Backend Enhancements (Optional)

These items are potential improvements that would reduce UI composition overhead; existing workflows already rely on consolidated endpoints.

### 1.1 Dedicated Admin Invitation Endpoint
- **Idea:** Introduce `POST /api/v1/admin/users/invite-admin` so invites no longer chain through organisation flows.
- **Benefits:** Cleaner audit trail, fewer UI steps, easier to monitor invite conversions.
- **Notes:** Should complement (not replace) the existing organisation invitation + domain assignment sequence.

### 1.2 Dashboard Activity Feed Endpoint
- **Status:** Delivered (`GET /api/v1/admin/dashboard/activity-feed`).
- **Next Enhancements:** Consider filtering by action type or exporting feeds for compliance tooling.

### 1.3 Organization Approval Queue Endpoints
- **Idea:** Add `GET /api/v1/admin/organizations/pending` and reuse `POST /api/v1/admin/organizations/{uuid}/moderate?action=approve|reject|revoke` to centralise approval flows.
- **Benefits:** Removes the need for client-side filtering and multi-call workflows around verification.

---

## 2. Frontend Enablement

### 2.1 Feature Gating
- Maintain flags for `activityFeed`, `organizationQueue`, and `adminInvite`.
- Default to `false` until optional dedicated endpoints reach production; keep consolidated flows active otherwise.

### 2.2 API Integration Checklist
- Wire `AdminInviteModal` to `POST /api/v1/admin/users/invite-admin` once available, then refetch:
  - `GET /api/v1/admin/users/admins`
  - `GET /api/v1/admin/users/eligible`
    - Ensure the dashboard consumes `GET /api/v1/admin/dashboard/activity-feed` for the activity timeline (in production).
- Populate the pending organizations tab using `GET /api/v1/admin/organizations/pending` and drive actions through `POST /api/v1/admin/organizations/{uuid}/moderate`.

### 2.3 Telemetry & QA
- Instrument new backend calls with audit events for invite issuance, approvals, and feed consumption.
- Extend the dashboard regression suite to cover the newly powered modules.

---

## 3. Tracking & Ownership

| Enhancement | Squad | Status |
|-------------|-------|--------|
| Dedicated admin invite endpoint | Tenancy/Identity | Evaluate |
| Activity feed endpoint | Tenancy/Identity | Delivered (main branch) |
| Organization approval queue endpoints | Tenancy/Identity | Delivered (main branch) |
| Frontend feature flag alignment | Web Platform | Active (supports both current and future flows) |

Update this file as milestones are scheduled or delivered.

---

## 4. Backlog Grooming Cadence

- **Monthly triage:** During each product/engineering sync, run through the optional enhancements above and decide whether product signals justify promoting any item into the active roadmap.
- **Readiness checklist:** Before promoting, confirm UX flows, DTO contracts, and acceptance criteria are captured in the relevant guide (`docs/guides/AdminDashboardDevelopmentGuide.md`).
- **Cross-squad visibility:** Log grooming outcomes in the squad planning board so Web Platform knows when to prep UI integration.
- **Retrospective review:** Every quarter, reassess whether consolidated endpoint usage still meets operator needs or if dedicated APIs should be elevated.

---

## 5. Feature Flag Stewardship

- **Flag register:** Keep `activityFeed`, `organizationQueue`, and `adminInvite` documented in the frontend configuration README with owners and default states.
- **Environment policy:** Default all three flags to `false` in production until the optional backend support is delivered; allow staging environments to toggle for exploratory testing.
- **Drift checks:** Add observability dashboards or CI checks that warn if a flag remains disabled after its corresponding enhancement ships.
- **Fallback discipline:** Retain the consolidated-flow code paths even after enabling new endpoints so operators can revert quickly if issues arise.
