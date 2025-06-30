-- V202506301839__Create_instructor_experience_table.sql
CREATE TABLE instructor_experience
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid     UUID         NOT NULL,
    position            VARCHAR(255) NOT NULL,
    organization_name   VARCHAR(255) NOT NULL,
    responsibilities    TEXT,
    years_of_experience DECIMAL(4, 2),
    start_date          DATE,
    end_date            DATE,
    is_current_position BOOLEAN               DEFAULT FALSE,
    created_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date        TIMESTAMP,
    created_by          VARCHAR(255) NOT NULL,
    updated_by          VARCHAR(255),

    CONSTRAINT fk_instructor_experience_instructor
        FOREIGN KEY (instructor_uuid) REFERENCES instructors (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_instructor_experience_instructor_id ON instructor_experience (instructor_uuid);
CREATE INDEX idx_instructor_experience_uuid ON instructor_experience (uuid);
CREATE INDEX idx_instructor_experience_years ON instructor_experience (years_of_experience);
CREATE INDEX idx_instructor_experience_current ON instructor_experience (is_current_position);