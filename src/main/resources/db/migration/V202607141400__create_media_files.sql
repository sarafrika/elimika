-- Central registry of every file persisted through the storage layer.
-- file_key is the canonical bare storage key relative to the storage root
-- (e.g. course_thumbnails/9f1b101a.jpg). Domain tables keep their own reference
-- columns holding the same key; this table adds metadata, ownership and
-- existence tracking for orphan/lost-file reconciliation.

CREATE TABLE media_files
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    file_key          TEXT         NOT NULL UNIQUE,
    original_filename TEXT,
    size_bytes        BIGINT,
    mime_type         VARCHAR(255),
    owner_type        VARCHAR(64)  NOT NULL,
    owner_uuid        UUID,
    file_exists       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP,
    created_by        VARCHAR(255) NOT NULL,
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_media_files_owner ON media_files (owner_type, owner_uuid);
CREATE INDEX idx_media_files_file_exists ON media_files (file_exists) WHERE NOT file_exists;

COMMENT ON TABLE media_files IS 'Registry of stored files: canonical key, metadata, owner and disk-existence flag';
COMMENT ON COLUMN media_files.file_key IS 'Bare storage key relative to the storage root, e.g. course_thumbnails/uuid.jpg';
COMMENT ON COLUMN media_files.owner_type IS 'Domain owner category, e.g. USER_PROFILE_IMAGE, COURSE_THUMBNAIL, LESSON_CONTENT';
COMMENT ON COLUMN media_files.file_exists IS 'FALSE when reconciliation found no file on disk for this key';
