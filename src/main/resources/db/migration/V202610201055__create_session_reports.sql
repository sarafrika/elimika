-- One mandatory report per class session: the instructor captures the session, previews it and submits it.
-- Once submitted it stays editable until editable_until (the school's grace period), then it locks.
CREATE TABLE session_reports
(
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    scheduled_instance_uuid UUID                     NOT NULL UNIQUE REFERENCES scheduled_instances (uuid) ON DELETE CASCADE,
    class_definition_uuid   UUID                     NOT NULL REFERENCES class_definitions (uuid) ON DELETE CASCADE,
    organisation_uuid       UUID,
    instructor_uuid         UUID,
    status                  VARCHAR(20)              NOT NULL DEFAULT 'draft',
    due_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at            TIMESTAMP WITH TIME ZONE,
    submitted_by_user_uuid  UUID REFERENCES users (uuid),
    editable_until          TIMESTAMP WITH TIME ZONE,
    revision_count          INTEGER                  NOT NULL DEFAULT 0,
    reminded_at             TIMESTAMP WITH TIME ZONE,
    overdue_notified_at     TIMESTAMP WITH TIME ZONE,
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date            TIMESTAMP WITH TIME ZONE,
    created_by              VARCHAR(255)             NOT NULL,
    updated_by              VARCHAR(255),
    CONSTRAINT chk_session_reports_status CHECK (LOWER(status) IN ('draft', 'submitted', 'locked'))
);

CREATE INDEX idx_session_reports_status_due ON session_reports (status, due_at);
CREATE INDEX idx_session_reports_org_status ON session_reports (organisation_uuid, status);
CREATE INDEX idx_session_reports_instructor_status ON session_reports (instructor_uuid, status);

-- Every submission (and the final state at lock time) is kept, never overwritten.
CREATE TABLE session_report_revisions
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    session_report_uuid    UUID                     NOT NULL REFERENCES session_reports (uuid) ON DELETE CASCADE,
    revision_number        INTEGER                  NOT NULL,
    snapshot               JSONB                    NOT NULL,
    submitted_by_user_uuid UUID REFERENCES users (uuid),
    submitted_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date           TIMESTAMP WITH TIME ZONE,
    created_by             VARCHAR(255)             NOT NULL,
    updated_by             VARCHAR(255),
    CONSTRAINT uq_session_report_revision UNIQUE (session_report_uuid, revision_number)
);
