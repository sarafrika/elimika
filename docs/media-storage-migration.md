# Media Storage Harmonization — Deployment Runbook

## What changed

All file storage now flows through one shared stack in `shared/storage`:

- **Canonical persisted value**: bare storage key (e.g. `course_thumbnails/<uuid>.jpg`).
  Public URLs (`/api/v1/files/<key>`) are produced at serialization time by
  `FileUrlResolver`; genuinely external URLs pass through untouched.
- **`media_files` registry**: every stored file is tracked (key, original name, size,
  MIME, owner type/uuid, `file_exists` flag).
- **`MediaStorageService` facade**: validate → store → register → delete-replaced-file.
  All module uploads (profile images, course/class media, lesson content, assignment
  attachments, certificates, credential documents) go through it.
- **Unified serving**: `GET /api/v1/files/{*key}` (public except `profile_documents/**`,
  which stays authenticated). The 9 legacy per-module media endpoints remain as thin
  delegates so previously issued URLs keep working.
- **Lifecycle**: replaced files are deleted from disk+registry; entity deletes remove
  their files; `MediaReconciliationService` handles orphans and lost files.

## Migrations (run automatically by Flyway)

| Migration | Purpose |
|---|---|
| `V202607141400__create_media_files.sql` | registry table |
| `V202607141405__normalize_media_file_references.sql` | rewrites every file-reference column to bare keys; originals preserved in `media_reference_backup` |
| `V202607141410__backfill_media_files_registry.sql` | populates the registry from domain rows |

The normalizer handles every format observed in production: absolute URLs on both
sarafrika.com hosts (with and without `/api/v1`), per-module API paths, %-encoded
segments, folder-less profile images, double-nested certificate paths, junk
placeholders (`/assignment.pdf` → NULL), empty strings → NULL, external links untouched.

## Deployment steps

1. **Backup** the database and the storage volume
   (`/var/lib/docker/volumes/elimika_elimika_storage/_data`).
2. *(Recommended)* Dry-run the migrations against a clone first:
   ```bash
   docker exec elimika-postgres psql -U admin -d postgres -c 'CREATE DATABASE elimika_dryrun;'
   docker exec elimika-postgres sh -c 'pg_dump -U admin elimika | psql -q -U admin -d elimika_dryrun'
   # apply the three V2026071414xx migration files with psql against elimika_dryrun,
   # then spot-check: no /api/v1/ or http://<self-host> values remain except external links
   ```
3. Deploy the new image; Flyway applies the migrations on boot.
4. **Reconcile against disk** (fills registry metadata, flags lost files):
   ```
   POST /api/v1/files/admin/reconcile
   ```
   Review the `deadReferences` list — production analysis (2026-07-14) found ~106 of
   ~149 references point to files lost before the storage volume mount existed.
5. **Prune dead references** so the API stops returning URLs that 404 (UI then shows
   placeholders and users can re-upload):
   ```
   POST /api/v1/files/admin/reconcile?prune=true
   ```
6. **Orphan sweep** (report-only first):
   ```
   POST /api/v1/files/admin/sweep            # report
   POST /api/v1/files/admin/sweep?deleteOrphans=true   # only after reviewing the report
   ```

## Rollback notes

- Original column values are preserved in `media_reference_backup`
  (table_name, column_name, row_uuid, old_value, new_value).
- The resolver tolerates unmigrated values (legacy `/api/v1/...` URLs pass through),
  so old-format data does not break the new code and vice versa.

## Follow-ups (later release)

- Remove the legacy flat-filename heuristics (`resolveCourseMediaPath`,
  `resolveClassMediaPath`) once media traffic on legacy endpoints goes quiet.
- Consider deprecation headers on the 9 legacy media endpoints.
- Frontend: media fields now return `/api/v1/files/<key>`; no action required, but new
  uploads should stop persisting full URLs into free-text fields.
