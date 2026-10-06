# Course Creator Onboarding

## Intent

Course creator onboarding starts in the Elimika UI, but personal identity is owned by Sarafrika Keycloak. Elimika stores the workspace-specific setup data: course categories, skills wallet evidence and verification state.

## Identity Boundary

Personal identifiable information belongs in Keycloak:

- first name
- last name
- email
- phone number
- password setup and email verification state

Elimika stores onboarding/workspace data:

- selected course categories
- skills wallet records and evidence
- verification submission and moderation state

The local `users` table is a read-only mirror of Keycloak identity. `course_creators.full_name` is derived from it by database triggers (on insert and whenever the user's names change) so catalogue and search can show creator names; onboarding never writes names or email itself.

## UI To API To Storage Flow

Registration and approval of the `course_creator` domain are described in
[registration-and-approval.md](registration-and-approval.md). Once registered, the creator fills in the
onboarding steps while their domain is pending:

```text
Course creator UI (signed in, account_state PENDING_APPROVAL)
  | GET  /api/v1/course-creators/me/onboarding            state, wallet progress, ready_for_submission
  | PUT  /api/v1/course-creators/me/onboarding/categories { category_uuids: [...] }
  | wallet tabs, all under /api/v1/course-creators/{courseCreatorUuid}:
  |   My Skills          /skills          (evidence, last_assessed_on)
  |   Portfolio          /portfolio
  |   Credentials Vault  /certifications  (credential_type)
  |   Competencies       /competencies
  |   Experience         /experience      (experience_type)
  |   Achievements       /achievements
  |   Verification       read-only: verification_status / is_verified set by admins
  | POST /api/v1/course-creators/me/onboarding/submit      -> SUBMITTED, admins notified
  v
Elimika storage
  | course_creators.verification_status and review timestamps
  | course_creator_category_preferences
  | user_skills | user_portfolio_items | user_certifications | user_competencies | user_experience | user_achievements
  |   (the user's shared wallet, see user-profile-and-wallet.md)
  v
Platform admin
  | POST /api/v1/admin/course-creators/{uuid}/wallet/{skills|competencies|certifications}/{itemUuid}/verification
  |        { status: VERIFIED | REJECTED, notes }
  | POST /api/v1/admin/course-creators/{uuid}/moderate?action=approve|reject|revoke
  v
course_creators.admin_verified + verification_status, and user_domain_mapping (course_creator) in step:
approve -> APPROVED, reject -> REJECTED, revoke -> SUSPENDED
```

## Review State

`course_creators.verification_status` is the review lifecycle:

- `DRAFT`: onboarding exists but has not been submitted
- `SUBMITTED`: creator sent the wallet for admin review
- `APPROVED`: admin approved the creator
- `REJECTED`: admin rejected the submission
- `REVOKED`: admin removed a previous approval

`admin_verified` remains for compatibility with existing access checks and search filters. New UI should prefer `verification_status` for onboarding screens and use `admin_verified` only when checking whether creator-only publishing features are available.

## Skills Wallet

Wallet progress counts the seven tabs that hold at least one item; the Verification tab counts once an
admin has verified any skill, competency or certification. Changing a skill's name or evidence, or a
competency's evidence, sends it back to `PENDING`. The wallet is the user's shared professional profile (see [user-profile-and-wallet.md](user-profile-and-wallet.md)), so items an instructor domain added count too. `user_skills.skill_name` remains the submitted free text, while `skill_uuid` points to the curated `skills` taxonomy when the name resolves by slug or alias.
