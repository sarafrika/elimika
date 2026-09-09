-- A tentative claim on an instructor's diary made when they apply for a marketplace
-- class job, firmed when they are hired and released once the class exists.

CREATE TABLE instructor_time_holds
(
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid         UUID         NOT NULL,
    instructor_user_uuid    UUID,
    organisation_uuid       UUID,
    job_uuid                UUID         NOT NULL,
    application_uuid        UUID         NOT NULL,
    title                   VARCHAR(255),
    start_time              TIMESTAMP    NOT NULL,
    end_time                TIMESTAMP    NOT NULL,
    timezone                VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    status                  VARCHAR(16)  NOT NULL,
    class_definition_uuid   UUID,
    scheduled_instance_uuid UUID,
    released_at             TIMESTAMP,
    release_reason          VARCHAR(255),
    created_date            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date            TIMESTAMP,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    CONSTRAINT chk_instructor_time_holds_status
        CHECK (status IN ('TENTATIVE', 'FIRM', 'CONFIRMED', 'RELEASED')),
    CONSTRAINT chk_instructor_time_holds_window CHECK (end_time > start_time)
);

-- Overlap-query workhorse: only TENTATIVE/FIRM rows can ever show on a diary.
CREATE INDEX idx_instructor_time_holds_diary
    ON instructor_time_holds (instructor_uuid, start_time, end_time)
    WHERE status IN ('TENTATIVE', 'FIRM');

CREATE INDEX idx_instructor_time_holds_application ON instructor_time_holds (application_uuid);
CREATE INDEX idx_instructor_time_holds_job ON instructor_time_holds (job_uuid, status);

-- No foreign keys to class_marketplace_jobs: resource_bookings takes none either, and a
-- hold outliving a purged job is harmless.
COMMENT ON TABLE instructor_time_holds IS 'Tentative and firm claims on an instructor diary raised by marketplace class job applications; never a real session';
COMMENT ON COLUMN instructor_time_holds.status IS 'TENTATIVE applied, FIRM hired, CONFIRMED became a scheduled instance, RELEASED dead. Only FIRM blocks scheduling';
