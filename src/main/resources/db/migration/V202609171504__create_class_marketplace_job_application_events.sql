-- The history of each marketplace job application: every transition with its actor, note and, for an
-- interview invitation, when the interview is. Mirrors training_application_events.

CREATE TABLE class_marketplace_job_application_events
(
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    application_uuid UUID         NOT NULL REFERENCES class_marketplace_job_applications (uuid) ON DELETE CASCADE,
    job_uuid         UUID         NOT NULL,
    event_type       VARCHAR(40)  NOT NULL,
    actor_uuid       UUID,
    actor_name       VARCHAR(255),
    note             TEXT,
    interview_at     TIMESTAMP,
    created_date     TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by       VARCHAR(255) NOT NULL,
    updated_date     TIMESTAMP,
    updated_by       VARCHAR(255),
    CONSTRAINT chk_class_marketplace_job_application_events_event_type
        CHECK (UPPER(event_type) IN ('APPLIED', 'REAPPLIED', 'SHORTLISTED', 'INTERVIEWING', 'OFFERED', 'HIRED',
                                     'ASSIGNED', 'REJECTED', 'NOT_SELECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_class_marketplace_job_application_events_application
    ON class_marketplace_job_application_events (application_uuid, created_date DESC);

-- Backfill, step 1: every existing application was applied for when it was created, by its instructor.
INSERT INTO class_marketplace_job_application_events
    (application_uuid, job_uuid, event_type, actor_uuid, actor_name, note, created_date, created_by)
SELECT a.uuid,
       a.job_uuid,
       'APPLIED',
       u.uuid,
       NULLIF(TRIM(CONCAT_WS(' ', u.first_name, u.last_name)), ''),
       a.application_note,
       a.created_date,
       'backfill'
FROM class_marketplace_job_applications a
         LEFT JOIN instructors i ON i.uuid = a.instructor_uuid
         LEFT JOIN users u ON u.uuid = i.user_uuid;

-- Step 2: an application past pending gets one event for the stage it is at now, stamped when it was
-- last reviewed. reviewed_by holds the reviewer's email; a withdrawal is always the instructor's own.
INSERT INTO class_marketplace_job_application_events
    (application_uuid, job_uuid, event_type, actor_uuid, actor_name, note, interview_at, created_date, created_by)
SELECT a.uuid,
       a.job_uuid,
       UPPER(a.status),
       actor.uuid,
       NULLIF(TRIM(CONCAT_WS(' ', actor.first_name, actor.last_name)), ''),
       a.review_notes,
       CASE WHEN UPPER(a.status) = 'INTERVIEWING' THEN a.interview_at END,
       GREATEST(a.created_date, COALESCE(a.reviewed_at, a.updated_date, a.created_date)),
       'backfill'
FROM class_marketplace_job_applications a
         LEFT JOIN instructors i ON i.uuid = a.instructor_uuid
         LEFT JOIN LATERAL (
    SELECT u.uuid, u.first_name, u.last_name
    FROM users u
    WHERE (UPPER(a.status) = 'WITHDRAWN' AND u.uuid = i.user_uuid)
       OR (UPPER(a.status) <> 'WITHDRAWN' AND u.email = a.reviewed_by)
    LIMIT 1
    ) actor ON TRUE
WHERE UPPER(a.status) <> 'PENDING';
