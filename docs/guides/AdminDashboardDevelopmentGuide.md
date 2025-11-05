# Elimika Admin Dashboard: Frontend Development Guide

## 1. Mission & Personas
The admin dashboard is the command center for keeping Elimika healthy, compliant, and growing. The primary persona is the **System Administrator** (global admin) with platform-wide privileges. Secondary personas, such as **Organization Admins**, benefit from shared components but have a scoped view of the same surfaces.

### Outcomes
- Real-time visibility into platform performance, growth, and operational risk.
- Ability to grant, review, and revoke privileged access without leaving the dashboard.
- Fast triage paths for organization and instructor verification workflows.
- Clear auditability of key admin actions using existing APIs and telemetry hooks.

---

## 2. Experience Blueprint ("360°" View)

### 2.1 Layout Shell
- **Global App Bar:** quick access to search, notifications, and profile controls. The bar persists across all admin workspaces.
- **Primary Navigation Rail:** anchors the five core surfaces: *Overview*, *Users*, *Organizations*, *Instructors*, and *Branches*. Each surface maps to concrete API contracts (see section 3).
- **Context Panel:** right-aligned panel for contextual insights (verification status, recent actions). Populated by responses from the relevant admin endpoints.
- **Workspace Canvas:** the central grid where KPI cards, tables, and detail views render.

### 2.2 Surface Summary
| Surface | Purpose | Primary Data Source |
|---------|---------|---------------------|
| Overview | KPIs, trend charts, system health snapshot, activity timeline | `GET /api/v1/admin/dashboard/statistics`, `GET /api/v1/admin/dashboard/activity-feed` |
| Users | Manage administrators and platform users | Admin user endpoints, `/api/v1/users*` |
| Organizations | Review organizations, moderate verification status, inspect members | Organization endpoints + admin verification routes |
| Instructors | Verify instructors, inspect documentation metrics | Admin instructor verification routes |
| Branches | View branches within organizations and their members | `/api/v1/organisations/{uuid}/training-branches*` |

### 2.3 Data Story
`AdminDashboardStatsDTO` already ships a comprehensive payload. UI teams should map the object to the following visual blocks:
- **User Metrics:** total users, 24h actives, new registrations (7d), suspended accounts.
- **Organization Metrics:** total organizations, pending approvals count, active vs suspended.
- **Admin Metrics:** total admins, breakdown of system vs organization admins, active sessions, admin actions today.
- **Learning & Timetabling Metrics:** course inventory, enrollments, session throughput, attendance trends.
- **Commerce Metrics:** total orders, last 30 days orders, captured orders, unique customers, purchase mix.
- **Communication Metrics:** notification creation/delivery/failure stats, pending count.
- **Compliance Metrics:** verification state for instructors and course creators, expiring documents.
- **System Performance:** uptime, response time, error rate, storage usage strings.

Keep chart transformations inside the presentation layer; the DTO already exposes aggregated values so no additional backend joins are required.

---

## 3. API Contract Map (Current Capabilities)
Only list APIs that exist in the service today.

### 3.1 Analytics & Overview
| Endpoint | Purpose | Notes |
|----------|---------|-------|
| `GET /api/v1/admin/dashboard/statistics` | Returns `AdminDashboardStatsDTO` for KPI, charts, and contextual panels | Cache for short intervals (30–60s) to reduce load |
| `GET /api/v1/admin/dashboard/activity-feed` | Returns paginated `AdminActivityEventDTO` items for the timeline | Sort by newest first; reuse pageable/sort controls |

### 3.2 Admin Identity & Domains
| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/admin/users/{uuid}/domains` | Grant admin domain (system or organisation scoped) |
| `DELETE /api/v1/admin/users/{uuid}/domains/{domain}` | Revoke admin domain |
| `GET /api/v1/admin/users/admins` | Paginated list of all admin users |
| `GET /api/v1/admin/users/system-admins` | Paginated list of system administrators |
| `GET /api/v1/admin/users/organization-admins` | Paginated list of organization administrators |
| `GET /api/v1/admin/users/eligible` | Paginated list of users eligible for promotion |
| `GET /api/v1/admin/users/{uuid}/is-admin` | Boolean check for any admin domain |
| `GET /api/v1/admin/users/{uuid}/is-system-admin` | Boolean check for system admin domain |

### 3.3 User Directory
| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/users` | Paginated list of users (supports Pageable params) |
| `GET /api/v1/users/search` | User search with query params |
| `GET /api/v1/users/{uuid}` | Fetch individual user profile |
| `PUT /api/v1/users/{uuid}` | Update user profile |
| `DELETE /api/v1/users/{uuid}` | Delete user |

### 3.4 Organization Oversight
| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/organisations` | Paginated organizations list |
| `POST /api/v1/organisations` | Create organization (admin bootstrap flow) |
| `GET /api/v1/organisations/{uuid}` | Organization details |
| `PUT /api/v1/organisations/{uuid}` | Update organization |
| `GET /api/v1/organisations/{uuid}/users` | Members inside organization |
| `GET /api/v1/organisations/{uuid}/training-branches` | Branch list under organization |
| `POST /api/v1/admin/organizations/{uuid}/moderate?action=approve|reject|revoke` | Unified moderation endpoint |
| `GET /api/v1/admin/organizations/{uuid}/verification-status` | Boolean verification check |
| `GET /api/v1/admin/organizations/pending` | Pending approval queue (unverified organisations) |

### 3.5 Instructor Verification & Compliance
| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/admin/instructors/{uuid}/verify` | Approve instructor |
| `POST /api/v1/admin/instructors/{uuid}/unverify` | Revoke verification |
| `GET /api/v1/admin/instructors/{uuid}/verification-status` | Boolean verification check |

### 3.6 Branch Operations
| Endpoint | Purpose |
|----------|---------|
| `POST /api/v1/organisations/{uuid}/training-branches` | Create branch inside organization |
| `GET /api/v1/organisations/{uuid}/training-branches/{branchUuid}` | Branch detail |
| `PUT /api/v1/organisations/{uuid}/training-branches/{branchUuid}` | Update branch |
| `DELETE /api/v1/organisations/{uuid}/training-branches/{branchUuid}` | Delete branch |
| `GET /api/v1/organisations/{uuid}/training-branches/{branchUuid}/users` | Users assigned to branch |
| `GET /api/v1/organisations/{uuid}/training-branches/{branchUuid}/users/domain/{domainName}` | Branch users filtered by domain |
| `POST /api/v1/organisations/{uuid}/training-branches/{branchUuid}/users/{userUuid}` | Assign user to branch |
| `DELETE /api/v1/organisations/{uuid}/training-branches/{branchUuid}/users/{userUuid}` | Remove user from branch |

---

## 4. Implementation Playbooks

### 4.1 Overview Surface
1. Fetch `GET /api/v1/admin/dashboard/statistics` on page load and via background refresh every 60 seconds (or on demand when filters change).
2. Map each nested metrics object to dedicated UI components (e.g., `user_metrics.total_users` to a KPI card).
3. Derive chart datasets client-side—e.g., convert `new_registrations_7d` into sparklines using historical snapshots stored in local state.
4. Surface status badges using `overall_health` and `system_performance` strings. Define thresholds client-side; the payload already normalises values as strings.
5. **Activity Feed:** pull paginated items from `GET /api/v1/admin/dashboard/activity-feed`, surface the `summary` as the primary title, and conditionally render badges for response status or action types.
   - `AdminActivityEventDTO` carries request metadata (method, endpoint, response status, processing time) plus actor details (`actor_name`, `actor_email`, `actor_domains`).
   - Use `occurred_at` (UTC) for ordering and `request_id` for linking deeper audit traces when troubleshooting.

### 4.2 Admin Identity Management
1. Use `GET /api/v1/admin/users/admins` as the primary source for the admin roster table. The response is a `PagedDTO`.
2. Filter views with supporting endpoints: system admins (`/system-admins`), organization admins (`/organization-admins`), and candidates (`/eligible`).
3. Wrap promotion actions around `POST /api/v1/admin/users/{uuid}/domains`. Required request payload mirrors `AdminDomainAssignmentRequestDTO` (domain name, assignment type, optional reason).
4. Wire demotion flows to `DELETE /api/v1/admin/users/{uuid}/domains/{domain}` and prompt for an optional reason query parameter.
5. Use `/is-admin` and `/is-system-admin` checks to gate destructive UI controls and to render inline badges in shared tables.

### 4.3 User Directory Operations
1. Back the global search bar with `GET /api/v1/users/search`. Pass raw query params (name, email, domain) as provided by the request builder.
2. Use `GET /api/v1/users` to populate default user tables and maintain pagination state.
3. Route profile drawers to `GET /api/v1/users/{uuid}` and basic edits to `PUT /api/v1/users/{uuid}`.
4. Deleting a user should call `DELETE /api/v1/users/{uuid}` and then invalidate local caches for admin lists (promotions may be impacted).

### 4.4 Organization Oversight
1. The Organization workspace lists entries from `GET /api/v1/organisations` and surfaces status columns from the DTO (e.g., `adminVerified`).
2. Back the approval queue tab with `GET /api/v1/admin/organizations/pending`; reuse the same table component and highlight verification reasons or submission timestamps if available.
3. Detail views should call `GET /api/v1/organisations/{uuid}` and hydrate tabs for *Members* (`GET /api/v1/organisations/{uuid}/users`) and *Branches* (`GET /api/v1/organisations/{uuid}/training-branches`).
4. Moderation actions call `POST /api/v1/admin/organizations/{uuid}/moderate` with `action=approve|reject|revoke`. Reflect the result in UI instantly and revalidate the overview stats.
5. Provide a small badge sourced from `GET /api/v1/admin/organizations/{uuid}/verification-status` for context panels.

### 4.5 Instructor Verification & Compliance
1. Instructor review queues can be powered by existing instructor search endpoints (outside this document) combined with verification calls below.
2. Verification toggles invoke `POST /api/v1/admin/instructors/{uuid}/verify` or `/unverify` and re-fetch compliance metrics via the statistics endpoint.
3. Display real-time status using `GET /api/v1/admin/instructors/{uuid}/verification-status` where a detail panel needs an authoritative badge.

### 4.6 Branch Management
1. Display branches under an organization with `GET /api/v1/organisations/{uuid}/training-branches`.
2. Provide branch-level drawers that call the detail endpoint and list assigned users. Cross-filter using `/users/domain/{domainName}` when highlighting specific roles (instructor view vs admin view).
3. Use the assignment endpoints to manage branch membership without leaving the dashboard surface.

---

## 5. State Management & Performance
- **Data Fetching Layer:** centralise API calls in a typed client (e.g., React Query + generated hooks). Keep polling for statistics separate from mutation flows so admin actions do not flood the dashboard endpoint.
- **Caching:** short-lived cache (<=60s) for statistics; longer cache for admins/users with explicit invalidation after mutations.
- **Optimistic UI:** apply for domain assignments/removals and verification toggles. Roll back on error with toast notification.
- **Pagination Normalisation:** reuse the shared `PagedDTO` mapper so tables across surfaces behave consistently.

## 6. Security & Access Control
- Guard admin routes with a custom hook (e.g., `useAdminAccess`) that inspects JWT claims for admin domains.
- Combine client-side gating with server responses: call `/is-admin` prior to showing privileged modals.
- Ensure CSRF and access tokens are scoped appropriately when embedding admin components inside other shells.

## 7. Error Handling & Observability
- Funnel API errors through a central handler that maps HTTP codes to actionable UI copy (403 -> "Insufficient privileges" etc.).
- Instrument critical actions (domain assignment, verification toggles) with analytics events for audit trails.
- Surface partial-data states on the overview screen when the statistics payload loads but a widget fails to render; do not block the rest of the dashboard.

## 8. Progressive Enhancements
- Keep emerging modules behind feature flags so you can iterate quickly while continuing to lean on the shared endpoints above.
- When product validation calls for additional APIs (e.g., richer moderation analytics, direct admin invites), coordinate delivery so the UI can drop interim compositions without regressions.

## 9. Frontend Preparation for Potential `POST /api/v1/admin/users/invite-admin`
The current invite workflow is fully supported by combining organization invitations with the domain-assignment endpoints. If a dedicated admin invite API is introduced later, the UI can adopt it with the following guardrails:

1. **Feature Flag:** wrap the invite flow in a `adminInvite.enabled` flag so it can flip from the consolidated approach to the dedicated endpoint at launch.
2. **Request Shape:** reuse the `AdminDomainAssignmentRequestDTO` fields (`domainName`, `assignmentType`, optional `reason`) and extend with invite-specific metadata (`email`, `fullName`) once the contract is formalised.
3. **Submission Flow:** on modal submit, call the new endpoint, display confirmation, and pre-fetch `/api/v1/admin/users/admins` plus `/eligible` to keep tables fresh.
4. **Audit Trail:** log invite attempts (success/failure) so admins can reconcile invites regardless of backend implementation.

## 10. Testing & Verification Checklist
- ✅ `GET /api/v1/admin/dashboard/statistics` renders KPI cards, charts, and contextual badges.
- ✅ Domain assignment and removal flows hit the correct admin endpoints and invalidate caches.
- ✅ Organization moderation actions (`approve`, `reject`, `revoke`) update verification badges and reflected metrics.
- ✅ Instructor verification toggles update compliance cards.
- ✅ Branch membership edits sync with backend endpoints and update branch tables.
- ✅ Activity feed renders using `GET /api/v1/admin/dashboard/activity-feed` and paginates correctly.
- ✅ Feature flags continue to guard optional modules (invite flow, future enhancements) without blocking core dashboards.
