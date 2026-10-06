# Registration and Approval

## Intent

People register in Elimika, not on the Keycloak sign-up page. They pick the domain they want (student,
instructor, course creator, parent or organisation), the backend creates their Keycloak account, and
Keycloak emails a link to set a password and verify the address. Students and parents are
active at once; instructors, course creators and organisations wait on the pending-approval screen until
a platform admin approves the domain.

## Identity Boundary

| Data | Owner | Where Elimika keeps it |
|---|---|---|
| Name, email, phone, date of birth, gender, password | Keycloak | `users` is a read-only mirror synced from Keycloak; profile edits go through `PUT /api/v1/users/{uuid}`, which writes to Keycloak |
| Requested domain, terms acceptance, set-password email timing | Elimika | `account_registrations` |
| Whether a domain grants access | Elimika | `user_domain_mapping.status` (`PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`) |
| Profile and review data (skills wallet, verification) | Elimika | domain tables such as `course_creators` |

Published events never carry personal data, because Spring Modulith stores them in `event_publication`
until they complete. The set-password email event carries only the Keycloak user id.

## UI ↔ API ↔ Storage Flow

```text
Registration form (UI)
  | POST /api/v1/registrations            public, rate limited, optional Turnstile captcha
  |   { first_name, last_name, email, phone_number, dob, gender, domain, terms_accepted }
  v
RegistrationService
  | 1. Keycloak admin API: create user (enabled, VERIFY_EMAIL + UPDATE_PASSWORD pending)
  | 2. one local transaction:
  |      users (mirror, keycloak_id linked)
  |      account_registrations (domain, terms_accepted_at)
  |      user_domain_mapping (PENDING if approval needed) -> admins notified
  |      UserDomainMappingEvent                     -> empty student/instructor/course creator profile
  |      RegistrationActionsEmailRequestedEvent     -> Keycloak executeActionsEmail after commit
  | 3. local transaction fails -> the Keycloak user is deleted again
  v
202 "If the address can be registered, a link is on its way" (same answer for known emails)

User sets password from the email, signs in (JWT)
  | GET /api/v1/users/me/account-status   -> account_state PENDING_APPROVAL | ACTIVE | REJECTED | SUSPENDED | NO_DOMAIN
  | any domain endpoint while pending     -> 403 { error: { code: "DOMAIN_PENDING_APPROVAL" } }
  | own onboarding/wallet endpoints        -> allowed (they check profile ownership, not the domain)
  v
Platform admin
  | GET  /api/v1/admin/registrations?status=PENDING&domain=
  | POST /api/v1/admin/users/{userUuid}/domains/{domain}/moderate?action=approve|reject|revoke
  |        instructor (students and parents need no approval)
  | POST /api/v1/admin/course-creators/{uuid}/moderate?action=approve|reject|revoke
  | POST /api/v1/admin/instructors/{uuid}/verify            (also approves the instructor domain)
  | POST /api/v1/admin/organizations/{uuid}/moderate?action=approve   (approves the org admins' domain)
  v
user_domain_mapping.status = APPROVED  -> user notified (DOMAIN_APPROVAL_GRANTED, in-app + email)
```

Existing Sarafrika accounts (another app on the shared realm) sign in and call
`POST /api/v1/registrations/me/domains { "domain": "instructor" }`; the domain is held pending the same way.
`POST /api/v1/registrations/resend { "email" }` re-sends the set-password email while it is outstanding.

## Configuration

| Property | Env var | Default |
|---|---|---|
| `app.registration.approval-required-domains` | `APP_REGISTRATION_APPROVAL_REQUIRED_DOMAINS` | `instructor,course_creator,organisation_user` |
| `app.registration.redirect-uri` | `APP_REGISTRATION_REDIRECT_URI` | `${app.email.frontend.url}/login` |
| `app.registration.client-id` | `APP_REGISTRATION_CLIENT_ID` | `elimika-ui` |
| `app.registration.actions-email-lifespan-seconds` | `APP_REGISTRATION_ACTIONS_EMAIL_LIFESPAN_SECONDS` | `259200` (72 h) |
| `app.registration.max-attempts-per-ip-per-hour` / `-per-email-per-hour` | `APP_REGISTRATION_MAX_ATTEMPTS_PER_*` | `10` / `3` |
| `app.registration.captcha.enabled` / `.secret` | `APP_REGISTRATION_CAPTCHA_ENABLED` / `_SECRET` | `false` |

## Keycloak Changes (ops)

These are realm settings, made by whoever administers the realm; the staging realm is shared with other
Sarafrika apps, so agree the change before applying it.

- Turn off self-registration (`registrationAllowed: false`) so sign-up only happens through Elimika.
- Turn on forgot-password (`resetPasswordAllowed: true`).
- Add the redirect URI above to the `elimika-ui` client's valid redirect URIs.
- The backend's service account needs the `manage-users` role of `realm-management`.

## Existing Data

Every domain mapping that existed before this change was backfilled as `APPROVED`, so no current user
lost access. Rows inserted outside the application default to `APPROVED`; the application always writes
an explicit status and creates new mappings as `PENDING` when the domain needs approval.
