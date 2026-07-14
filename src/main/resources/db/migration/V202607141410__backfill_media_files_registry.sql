-- Populates the media_files registry from every domain column that references a
-- stored file. Runs after V202607141405 normalized those columns to bare keys, so
-- anything still starting with http(s):// or '/' is external/unrecognized and skipped.
-- size_bytes / mime_type are filled where the owning table has them; the
-- reconciliation job fills the rest (and the file_exists flag) by statting disk.

CREATE OR REPLACE FUNCTION pg_temp.is_storage_key(v TEXT) RETURNS BOOLEAN AS
$$
SELECT v IS NOT NULL AND v <> '' AND v !~* '^https?://' AND left(v, 1) <> '/';
$$ LANGUAGE sql IMMUTABLE;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT profile_image_url, NULL, 'USER_PROFILE_IMAGE', uuid, 'system:media-backfill'
FROM users
WHERE pg_temp.is_storage_key(profile_image_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT thumbnail_url, NULL, 'COURSE_THUMBNAIL', uuid, 'system:media-backfill'
FROM courses
WHERE pg_temp.is_storage_key(thumbnail_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT banner_url, NULL, 'COURSE_BANNER', uuid, 'system:media-backfill'
FROM courses
WHERE pg_temp.is_storage_key(banner_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT intro_video_url, NULL, 'COURSE_INTRO_VIDEO', uuid, 'system:media-backfill'
FROM courses
WHERE pg_temp.is_storage_key(intro_video_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_url, NULL, file_size_bytes, mime_type, 'LESSON_CONTENT', uuid, 'system:media-backfill'
FROM lesson_contents
WHERE pg_temp.is_storage_key(file_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT thumbnail_url, NULL, 'CLASS_THUMBNAIL', uuid, 'system:media-backfill'
FROM class_definitions
WHERE pg_temp.is_storage_key(thumbnail_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT promotional_video_url, NULL, 'CLASS_PROMO_VIDEO', uuid, 'system:media-backfill'
FROM class_definitions
WHERE pg_temp.is_storage_key(promotional_video_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT certificate_url, NULL, 'CERTIFICATE', uuid, 'system:media-backfill'
FROM certificates
WHERE pg_temp.is_storage_key(certificate_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT background_image_url, NULL, 'CERTIFICATE_TEMPLATE_BACKGROUND', uuid, 'system:media-backfill'
FROM certificate_templates
WHERE pg_temp.is_storage_key(background_image_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_url, original_filename, file_size_bytes, mime_type, 'ASSIGNMENT_ATTACHMENT', uuid, 'system:media-backfill'
FROM assignment_attachments
WHERE pg_temp.is_storage_key(file_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_url, original_filename, file_size_bytes, mime_type, 'ASSIGNMENT_SUBMISSION_ATTACHMENT', uuid, 'system:media-backfill'
FROM assignment_submission_attachments
WHERE pg_temp.is_storage_key(file_url)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_path, title, file_size, mime_type, 'CLASS_RESOURCE', uuid, 'system:media-backfill'
FROM class_resources
WHERE pg_temp.is_storage_key(file_path)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_path, original_filename, file_size_bytes, mime_type, 'INSTRUCTOR_DOCUMENT', instructor_uuid, 'system:media-backfill'
FROM instructor_documents
WHERE pg_temp.is_storage_key(file_path)
ON CONFLICT (file_key) DO NOTHING;

INSERT INTO media_files (file_key, original_filename, size_bytes, mime_type, owner_type, owner_uuid, created_by)
SELECT file_path, original_filename, file_size_bytes, mime_type, 'COURSE_CREATOR_DOCUMENT', course_creator_uuid, 'system:media-backfill'
FROM course_creator_documents
WHERE pg_temp.is_storage_key(file_path)
ON CONFLICT (file_key) DO NOTHING;

-- Submission file_urls arrays reference storage keys after normalization
INSERT INTO media_files (file_key, original_filename, owner_type, owner_uuid, created_by)
SELECT DISTINCT ON (u) u, NULL, 'ASSIGNMENT_SUBMISSION_ATTACHMENT', s.uuid, 'system:media-backfill'
FROM assignment_submissions s, unnest(s.file_urls) AS u
WHERE pg_temp.is_storage_key(u)
ON CONFLICT (file_key) DO NOTHING;
