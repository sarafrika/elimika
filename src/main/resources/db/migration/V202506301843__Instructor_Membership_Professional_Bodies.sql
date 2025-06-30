-- V202506301845__Create_instructor_professional_memberships_table.sql
CREATE TABLE instructor_professional_memberships
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid   UUID         NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    membership_number VARCHAR(100),
    start_date        DATE,
    end_date          DATE,
    is_active         BOOLEAN               DEFAULT TRUE,
    created_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP,
    created_by        VARCHAR(255) NOT NULL,
    updated_by        VARCHAR(255),

    CONSTRAINT fk_instructor_memberships_instructor
        FOREIGN KEY (instructor_uuid) REFERENCES instructors (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_instructor_memberships_instructor_id ON instructor_professional_memberships (instructor_uuid);
CREATE INDEX idx_instructor_memberships_uuid ON instructor_professional_memberships (uuid);
CREATE INDEX idx_instructor_memberships_active ON instructor_professional_memberships (is_active);