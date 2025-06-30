-- V202506301830__Create_instructor_education_table.sql
CREATE TABLE instructor_education
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid    UUID         NOT NULL,
    qualification      VARCHAR(255) NOT NULL,
    school_name        VARCHAR(255) NOT NULL,
    year_completed     INTEGER,
    certificate_number VARCHAR(100),
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255),

    CONSTRAINT fk_instructor_education_instructor
        FOREIGN KEY (instructor_uuid) REFERENCES instructors (uuid) ON DELETE CASCADE
);