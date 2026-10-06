# User Professional Profile and Skills Wallet

## Intent

A user can hold several domains (student, instructor, course_creator, organisation_user, parent). Professional
data belongs to the **user**, not to a domain: bio, headline, website, location, skills, education, experience,
memberships, credentials, portfolio, competencies, achievements and credential documents are entered once and
every domain reads the same copy. Verification is per item and holds for every domain.

The `profile` module (`apps.sarafrika.elimika.profile`) owns this data. Other modules use it only through
`profile :: profile-spi` (`ProfessionalProfileService`, `ProfileSectionService`, DTOs, enums and
`ProfessionalProfileChangedEvent`).

## UI To API To Storage Flow

```text
Profile UI (any signed-in user, whatever domain is active)
  | GET    /api/v1/me/profile                      basics + section_counts + completeness_percent
  | PUT    /api/v1/me/profile                      { bio, professional_headline, website, location_name, latitude, longitude }
  | GET|POST          /api/v1/me/profile/{section}
  | PUT|DELETE        /api/v1/me/profile/{section}/{itemUuid}
  |   section = skills | education | experience | memberships | certifications
  |             | portfolio | competencies | achievements
  | POST   /api/v1/me/profile/documents            multipart: file, document_type_uuid, title, description,
  |                                                 education_uuid | experience_uuid | membership_uuid, expiry_date
  | PUT|DELETE /api/v1/me/profile/documents/{uuid}  metadata only; the file stays
  | GET    /api/v1/me/profile/documents/{uuid}/file
  v
profile module (ProfessionalProfileService)
  | one row per user:  user_professional_profiles
  | user-keyed items:  user_skills | user_education | user_experience | user_memberships | user_certifications
  |                    user_portfolio_items | user_competencies | user_achievements | user_documents
  | files:             MediaStorageService under profile_documents/users/{userUuid}/, media_files owner USER_DOCUMENT
  | publishes ProfessionalProfileChangedEvent(user, section, basics) inside the transaction
  v
Domain modules (listeners, same transaction)
  | instructor:     copies basics onto instructors.*, re-indexes the instructor on skills/experience changes
  | course creator: copies basics onto course_creators.*

Viewer UI (org staff, reviewer, admin)
  | GET /api/v1/users/{userUuid}/profile[/{section}]
  | GET /api/v1/users/{userUuid}/profile/documents/{uuid}/file
  |   allowed: the user, a platform admin, staff of an organisation the user belongs to,
  |            or a reviewer of an application the user lodged as an instructor

Admin UI
  | POST /api/v1/admin/users/{userUuid}/profile/{skills|certifications|competencies|documents}/{itemUuid}/verification
  |      { status: VERIFIED | REJECTED, notes }
  v
  verification_status / verified_at / verification_notes on the item (documents: is_verified + status)
```

## Legacy Domain Routes

The instructor and course creator routes keep their request and response shapes; their services now read
and write the shared `user_*` tables by mapping the profile UUID to its owner:

| Legacy route | Shared section |
| --- | --- |
| `/api/v1/instructors/{id}/skills, /education, /experience, /memberships, /documents` | skills, education, experience, memberships, documents |
| `/api/v1/course-creators/{id}/skills, /education, /experience, /memberships, /certifications, /documents` | same sections plus certifications |
| `/api/v1/course-creators/{id}/portfolio, /competencies, /achievements` | portfolio, competencies, achievements |
| `/api/v1/admin/course-creators/{id}/wallet/{section}/{item}/verification` | the admin verification above |

So a skill an instructor adds shows on their course creator wallet with the same `uuid`, and an admin
verdict on it shows in both. `instructor_uuid` / `course_creator_uuid` in responses is the domain profile of
the item's owner. Instructor and course creator `bio`, `professional_headline`, `website`, `location_name`,
`latitude` and `longitude` write through to `user_professional_profiles`, so editing one domain updates the
other. New UI should use `/api/v1/me/profile` and `/api/v1/users/{userUuid}/profile`.

## Rules

- **One claim per user.** Adding an item identical to an existing one (normalised name or key fields, any
  spacing or case) updates the existing item instead of adding a duplicate; blank fields keep their values.
  Skills are unique per user in the database.
- **Verification is admin-only and per item.** Only platform admins set `VERIFIED` or `REJECTED`; an admin who
  also holds a domain cannot verify their own items. Changing a claim resets it to `PENDING`: a skill's name or
  evidence, a competency's name or evidence, a credential's name, issuer, credential id or URL.
- **Documents** link optionally to one of the owner's education, experience or membership records; a link to
  someone else's record is rejected.
- **Completeness** (`completeness_percent`) counts basics (bio and headline) plus each of the nine sections that
  holds an item. Profile-completion reminders use the same bio + headline rule.

## Storage and Migration

- `V202610061243__create_user_professional_profile.sql` creates the tables (FK `user_uuid -> users(uuid)`
  `ON DELETE CASCADE`) and widens `instructors.professional_headline` / `website` to 500.
- `V202610061244__backfill_user_professional_profile.sql` copies `instructor_*` and `course_creator_*` rows into
  `user_*` by owner, collapsing identical rows per user. The instructor row's UUID wins, so existing references keep
  resolving; a collapsed course creator row contributes its evidence, verdict, taxonomy link or experience type.
  Document links follow the education/experience/membership row they collapsed into. Basics prefer non-blank
  values, instructor first, and are copied back onto both domain rows.
- The legacy `instructor_*` and `course_creator_*` qualification tables are kept but no longer read or written.

## Readers Switched to the Shared Profile

Instructor search documents and job matching (skills, experience, basics), the organisation instructor summary,
credential-read checks on instructor and course creator routes, admin dashboard document counts, media
reconciliation (`user_documents.file_path`), course creator wallet progress and submission readiness, and
profile-completion reminders.
