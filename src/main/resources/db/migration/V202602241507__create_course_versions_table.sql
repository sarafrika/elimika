-- Capture immutable publish snapshots for courses.

CREATE TABLE course_versions
(
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_uuid           UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    version_number        INTEGER                  NOT NULL,
    snapshot_hash         VARCHAR(64)              NOT NULL,
    snapshot_payload_json JSONB                    NOT NULL,
    published_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date          TIMESTAMP WITH TIME ZONE,
    created_by            VARCHAR(255)             NOT NULL,
    updated_by            VARCHAR(255),
    CONSTRAINT uq_course_versions_number UNIQUE (course_uuid, version_number),
    CONSTRAINT uq_course_versions_hash UNIQUE (course_uuid, snapshot_hash),
    CONSTRAINT chk_course_versions_version_number_positive CHECK (version_number > 0)
);

CREATE INDEX idx_course_versions_course_uuid ON course_versions (course_uuid);
CREATE INDEX idx_course_versions_published_at ON course_versions (published_at DESC);
