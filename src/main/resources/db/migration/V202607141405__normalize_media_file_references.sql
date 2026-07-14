-- Normalizes every persisted file reference to the canonical bare storage key
-- (e.g. course_thumbnails/uuid.jpg). Historical rows contain a mix of formats
-- observed in production:
--   1. bare keys                          course_thumbnails/uuid.jpg
--   2. app-relative API URLs              /api/v1/courses/media/<enc-path>
--   3. absolute self-host URLs            https://api.elimika[.staging].sarafrika.com/api/v1/...
--   4. absolute self-host URLs w/o /api/v1  https://host/assignments/..., https://host/course_materials/...
--   5. profile images stored w/o folder   /api/v1/users/profile-image/<file> -> profile_images/<file>
--   6. double-nested certificate URLs     /api/v1/certificates/files/certificates/uuid.pdf
--   7. junk placeholders / empty strings  '/assignment.pdf', '' -> NULL
--   8. genuinely external URLs            left untouched
-- Original values are preserved in media_reference_backup for audit/rollback.

CREATE TABLE IF NOT EXISTS media_reference_backup
(
    id           BIGSERIAL PRIMARY KEY,
    table_name   TEXT      NOT NULL,
    column_name  TEXT      NOT NULL,
    row_uuid     UUID,
    old_value    TEXT,
    new_value    TEXT,
    migrated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- % -decoding helper (controllers persisted UriUtils.encodePath output)
CREATE OR REPLACE FUNCTION pg_temp.url_decode(input TEXT) RETURNS TEXT AS
$$
DECLARE
    result TEXT := '';
    remainder TEXT := input;
    hex TEXT;
BEGIN
    IF input IS NULL OR position('%' IN input) = 0 THEN
        RETURN input;
    END IF;
    WHILE length(remainder) > 0 LOOP
        IF left(remainder, 1) = '%' AND length(remainder) >= 3 THEN
            hex := substr(remainder, 2, 2);
            IF hex ~ '^[0-9A-Fa-f]{2}$' THEN
                result := result || convert_from(decode(hex, 'hex'), 'UTF8');
                remainder := substr(remainder, 4);
                CONTINUE;
            END IF;
        END IF;
        result := result || left(remainder, 1);
        remainder := substr(remainder, 2);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION pg_temp.normalize_media_ref(input TEXT) RETURNS TEXT AS
$$
DECLARE
    v TEXT := trim(coalesce(input, ''));
    remainder TEXT;
BEGIN
    IF v = '' THEN
        RETURN NULL;
    END IF;

    -- Junk placeholder seen in assignment_submissions.file_urls
    IF v = '/assignment.pdf' THEN
        RETURN NULL;
    END IF;

    -- Absolute self-host URL: strip scheme+host, keep the path
    IF v ~* '^https?://[^/]*\.?sarafrika\.com(/|$)' THEN
        v := regexp_replace(v, '^https?://[^/]+', '', 'i');
        IF v = '' THEN
            RETURN NULL;
        END IF;
    ELSIF v ~* '^https?://' THEN
        -- Genuinely external URL: leave untouched
        RETURN input;
    END IF;

    -- Profile images were persisted without their folder
    IF v LIKE '/api/v1/users/profile-image/%' THEN
        RETURN 'profile_images/' || pg_temp.url_decode(substr(v, length('/api/v1/users/profile-image/') + 1));
    END IF;

    -- Certificates: strip endpoint prefix, re-prefix folder when flat
    IF v LIKE '/api/v1/certificates/files/%' THEN
        remainder := pg_temp.url_decode(ltrim(substr(v, length('/api/v1/certificates/files/') + 1), '/'));
        IF remainder = '' THEN
            RETURN NULL;
        END IF;
        IF remainder NOT LIKE 'certificates/%' THEN
            remainder := 'certificates/' || remainder;
        END IF;
        RETURN remainder;
    END IF;

    -- Instructor / course-creator document URLs
    IF v ~ '^/api/v1/(instructors|course-creators)/[^/]+/documents/files/' THEN
        RETURN pg_temp.url_decode(
            ltrim(regexp_replace(v, '^/api/v1/(instructors|course-creators)/[^/]+/documents/files/', ''), '/'));
    END IF;

    -- Remaining per-module endpoint prefixes: strip and decode
    v := regexp_replace(v,
        '^/api/v1/(courses/content-media|courses/media|classes/media|assignments/submission-media|assignments/media|files)/',
        '');
    v := pg_temp.url_decode(ltrim(v, '/'));

    IF v = '' THEN
        RETURN NULL;
    END IF;

    -- Whatever remains must look like a storage key (folder/file or bare file);
    -- reject leftover API paths we failed to recognize so they surface in review
    IF v LIKE 'api/v1/%' THEN
        RETURN input;
    END IF;

    RETURN v;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Normalizes one table.column in place, saving changed values to the backup table
CREATE OR REPLACE FUNCTION pg_temp.normalize_column(p_table TEXT, p_column TEXT) RETURNS INTEGER AS
$$
DECLARE
    changed INTEGER;
BEGIN
    EXECUTE format(
        'WITH changes AS (
             SELECT uuid AS row_uuid, %1$I AS old_value, pg_temp.normalize_media_ref(%1$I) AS new_value
             FROM %2$I
             WHERE %1$I IS NOT NULL
               AND %1$I IS DISTINCT FROM pg_temp.normalize_media_ref(%1$I)
         ),
         backup AS (
             INSERT INTO media_reference_backup (table_name, column_name, row_uuid, old_value, new_value)
             SELECT %2$L, %1$L, row_uuid, old_value, new_value FROM changes
         )
         UPDATE %2$I t
         SET %1$I = c.new_value
         FROM changes c
         WHERE t.uuid = c.row_uuid',
        p_column, p_table);
    GET DIAGNOSTICS changed = ROW_COUNT;
    RAISE NOTICE 'normalized %.%: % rows', p_table, p_column, changed;
    RETURN changed;
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.normalize_column('users', 'profile_image_url');
SELECT pg_temp.normalize_column('courses', 'thumbnail_url');
SELECT pg_temp.normalize_column('courses', 'banner_url');
SELECT pg_temp.normalize_column('courses', 'intro_video_url');
SELECT pg_temp.normalize_column('lesson_contents', 'file_url');
SELECT pg_temp.normalize_column('class_definitions', 'thumbnail_url');
SELECT pg_temp.normalize_column('class_definitions', 'promotional_video_url');
SELECT pg_temp.normalize_column('certificates', 'certificate_url');
SELECT pg_temp.normalize_column('certificate_templates', 'background_image_url');
SELECT pg_temp.normalize_column('assignment_attachments', 'file_url');
SELECT pg_temp.normalize_column('assignment_attachments', 'stored_filename');
SELECT pg_temp.normalize_column('assignment_submission_attachments', 'file_url');
SELECT pg_temp.normalize_column('assignment_submission_attachments', 'stored_filename');
SELECT pg_temp.normalize_column('class_resources', 'file_path');
SELECT pg_temp.normalize_column('instructor_documents', 'file_path');
SELECT pg_temp.normalize_column('instructor_documents', 'stored_filename');
SELECT pg_temp.normalize_column('course_creator_documents', 'file_path');
SELECT pg_temp.normalize_column('course_creator_documents', 'stored_filename');

-- assignment_submissions.file_urls is TEXT[]; normalize element-wise, dropping
-- junk entries that normalize to NULL
WITH changes AS (
    SELECT s.uuid AS row_uuid,
           s.file_urls AS old_value,
           (SELECT array_agg(n ORDER BY ord)
            FROM unnest(s.file_urls) WITH ORDINALITY AS t(u, ord)
            CROSS JOIN LATERAL (SELECT pg_temp.normalize_media_ref(u) AS n) x
            WHERE x.n IS NOT NULL) AS new_value
    FROM assignment_submissions s
    WHERE s.file_urls IS NOT NULL
),
filtered AS (
    SELECT * FROM changes WHERE old_value IS DISTINCT FROM new_value
),
backup AS (
    INSERT INTO media_reference_backup (table_name, column_name, row_uuid, old_value, new_value)
    SELECT 'assignment_submissions', 'file_urls', row_uuid,
           array_to_string(old_value, E'\n'), array_to_string(new_value, E'\n')
    FROM filtered
)
UPDATE assignment_submissions s
SET file_urls = f.new_value
FROM filtered f
WHERE s.uuid = f.row_uuid;

-- Blank-string cleanup: normalize_media_ref already maps '' -> NULL via the updates
-- above (blank values were caught by the IS DISTINCT FROM predicate).
