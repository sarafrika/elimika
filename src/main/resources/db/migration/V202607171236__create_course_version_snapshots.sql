-- Append-only content history for courses.
--
-- content_moderation_history records moderation *decisions* (who approved/rejected and why)
-- but stores no content. This table records the course *content* itself: a snapshot of the
-- full live tree is written each time an edit is promoted, so there is a durable record of
-- what a course looked like at each approved version.
--
-- Not required for reject-revert: a rejected edit is discarded from the draft row and the
-- live course is never mutated, so there is nothing to roll back.

CREATE TABLE course_version_snapshots
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    course_uuid       UUID                     NOT NULL REFERENCES courses (uuid) ON DELETE CASCADE,
    version_number    INTEGER                  NOT NULL,
    snapshot          JSONB                    NOT NULL,
    pending_edit_uuid UUID REFERENCES course_pending_edits (uuid) ON DELETE SET NULL,

    created_date      TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date      TIMESTAMP WITH TIME ZONE,
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255)
);

CREATE UNIQUE INDEX uq_cvs_course_version ON course_version_snapshots (course_uuid, version_number);
CREATE INDEX idx_cvs_course ON course_version_snapshots (course_uuid, created_date DESC);

COMMENT ON TABLE course_version_snapshots IS
    'Append-only history of approved course content. One row per promoted version.';
COMMENT ON COLUMN course_version_snapshots.snapshot IS
    'Full course tree at this version: course fields, category_uuids, lessons and their content. Media fields hold storage keys, not resolved URLs.';
COMMENT ON COLUMN course_version_snapshots.version_number IS
    'Monotonic per course, starting at 1 for the first approved version.';
COMMENT ON COLUMN course_version_snapshots.pending_edit_uuid IS
    'The edit whose promotion produced this version. NULL for a snapshot not created by an edit.';
