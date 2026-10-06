# Student Guardians

## Intent

Parents never self-register. The only way a parent gets access is that a student names them during
onboarding (or later on their profile). For each guardian the student gives a name, an email
(required), an optional mobile number and a relationship (`PARENT`, `GUARDIAN` or `SPONSOR`; defaults
to `GUARDIAN`). At most two guardians.

- **The email already has an Elimika account.** The guardian is linked at once: an `ACTIVE`
  `student_guardian_links` row, the `parent` domain granted (no admin approval), and an in-app and
  email notice (`GUARDIAN_LINK_ESTABLISHED`).
- **The email has no account.** The guardian is emailed an invitation (`GUARDIAN_LINK_INVITATION`)
  with a link that is valid for 14 days. From the link they either sign in and accept, or create an
  account for that email and are linked in the same step.

The organisation-invitation consent flow for minors (`/api/v1/guardian-invitations/...`) is
separate and unchanged; a guardian who consents there also ends up with an active link, and the
student's guardian list shows them as `linked`.

## UI ↔ API ↔ Storage Flow

```text
Student onboarding / profile form (UI)
  | POST /api/v1/students                 (create)
  | PUT  /api/v1/students/{uuid}          (update)
  |   { ..., "guardians": [ { name, email, phone?, relationship_type? } ] }   max 2, write-only
  |   guardians omitted  -> guardians untouched (legacy first/second_guardian_* fields still work)
  |   guardians: []      -> pending invitations withdrawn
  v
StudentServiceImpl -> StudentGuardianProvisioningService.syncGuardians
  | students.guardian_1_name/_mobile, guardian_2_name/_mobile   mirrored from guardians[0], [1]
  | student_guardian_contacts (one live row per student + email)
  |   email known?  UserLookupService.findUserUuidByEmail
  |     yes -> GuardianAccessService.createOrUpdateLink
  |              student_guardian_links (ACTIVE, share_scope FULL)
  |              UserDomainMappingEvent(parent) -> user_domain_mapping APPROVED (no approval needed)
  |              contact_status = LINKED, link_uuid set
  |              NotificationRequestedEvent GUARDIAN_LINK_ESTABLISHED (in_app + email)
  |     no  -> contact_status = INVITED, token_hash = sha256(token), invitation_expires_at = now + 14d
  |              NotificationRequestedEvent GUARDIAN_LINK_INVITATION (email)
  |                link: {frontend}/guardian-links/{token}
  |   same email saved again  -> details updated, nothing re-sent, no duplicate link
  |   guardian dropped        -> contact_status = REMOVED, token cleared (invite link dies);
  |                              an ACTIVE link is NOT revoked (use DELETE /api/v1/guardians/links/{linkUuid})
  v
Student / org staff / admin view (UI)
  | GET  /api/v1/students/{uuid}/guardians
  |   -> [{ uuid, name, email, phone, relationship_type,
  |         status: linked | invited | expired | declined | revoked,
  |         guardian_user_uuid, link_uuid, invitation_sent_at, invitation_expires_at, linked_at }]
  |   guardians linked another way (org consent, staff) appear with uuid = null
  | POST /api/v1/students/{uuid}/guardians/{guardianUuid}/resend-invitation
  |   -> new token (old link stops working), expiry restarted, email re-sent; 409 if already linked,
  |      or if the last send was under a minute ago

Guardian opens the emailed link (UI page /guardian-links/{token})
  | GET  /api/v1/student-guardian-invitations/token/{token}          public
  |   -> { student_name, guardian_name, masked_guardian_email, relationship_type,
  |        status, actionable, has_account, expires_at }
  |
  | has_account = true  -> sign in, then
  |   POST /api/v1/student-guardian-invitations/token/{token}/accept   (signed-in email must match)
  | has_account = false ->
  |   POST /api/v1/student-guardian-invitations/token/{token}/register public
  |     { first_name, last_name, phone_number?, terms_accepted: true }
  |     tenancy GuardianAccountService: Keycloak user for the INVITED email (no domain requested),
  |       users + account_registrations(requested_domain = parent), set-password email
  |     then the link is created as above (parent domain granted)
  | POST /api/v1/student-guardian-invitations/token/{token}/decline  public
  v
student_guardian_contacts.contact_status = LINKED | DECLINED, token_hash cleared

Signed-in guardian who lost the email
  | GET  /api/v1/student-guardian-invitations/me                     open invites for my email
  | POST /api/v1/student-guardian-invitations/{uuid}/accept | /decline
```

## Who May Do What

| Action | Allowed |
|---|---|
| Send `guardians` on create | the learner, a manager of one of their organisations, platform admin (`canCreateStudentFor`) |
| Send `guardians` on update | the learner, a manager of one of their organisations, platform admin (`canManageGuardianLinksFor`); existing guardians may edit the profile but not name further guardians |
| `GET .../guardians` | the learner, an active guardian, a manager of one of their organisations, platform admin (`canViewGuardianDetails`) |
| `POST .../resend-invitation` | same as sending `guardians` on update |
| Accept by token or uuid | only an account whose email equals the invited email |
| Register from token, decline by token | anyone holding the emailed token (the account is always created for the invited email, so only its mailbox can set the password) |

## Storage

`student_guardian_contacts` (migration `V202610061240__create_student_guardian_contacts.sql`):
`student_uuid`, `guardian_email` (lower-case), `guardian_name`, `guardian_phone`,
`relationship_type`, `position`, `contact_status` (`INVITED`, `LINKED`, `DECLINED`, `REMOVED`),
`guardian_user_uuid`, `link_uuid`, `token_hash` (SHA-256 only; the raw token exists only in the
email), `invitation_expires_at`, `invitation_sent_at`, `invitation_send_count`, `linked_at`,
`declined_at`, `removed_at`, `invited_by`. A partial unique index keeps one live row per student and
email. `expired` is derived (`INVITED` past `invitation_expires_at`); `revoked` is reported when
the link behind a `LINKED` row is no longer active.

Notification types `GUARDIAN_LINK_INVITATION` and `GUARDIAN_LINK_ESTABLISHED` are added to both
CHECK constraints in `V202610061241__add_student_guardian_notification_types.sql`.
