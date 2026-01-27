-- 202601271021__create_assignment_attachment_tables.sql
-- Create attachment tables for assignments and assignment submissions

CREATE TABLE assignment_attachments
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    assignment_uuid    UUID                     NOT NULL REFERENCES assignments (uuid) ON DELETE CASCADE,
    original_filename  TEXT,
    stored_filename    TEXT                     NOT NULL,
    file_url           TEXT                     NOT NULL,
    file_size_bytes    BIGINT                   NOT NULL,
    mime_type          VARCHAR(255)             NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255)
);

CREATE TABLE assignment_submission_attachments
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    submission_uuid    UUID                     NOT NULL REFERENCES assignment_submissions (uuid) ON DELETE CASCADE,
    original_filename  TEXT,
    stored_filename    TEXT                     NOT NULL,
    file_url           TEXT                     NOT NULL,
    file_size_bytes    BIGINT                   NOT NULL,
    mime_type          VARCHAR(255)             NOT NULL,
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255)
);

CREATE INDEX idx_assignment_attachments_uuid ON assignment_attachments (uuid);
CREATE INDEX idx_assignment_attachments_assignment_uuid ON assignment_attachments (assignment_uuid);

CREATE INDEX idx_assignment_submission_attachments_uuid ON assignment_submission_attachments (uuid);
CREATE INDEX idx_assignment_submission_attachments_submission_uuid ON assignment_submission_attachments (submission_uuid);
