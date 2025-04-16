-- Create table for students
CREATE TABLE students
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    full_name         VARCHAR(255) NOT NULL,
    guardian_1_name   VARCHAR(255),
    guardian_1_mobile VARCHAR(15),
    guardian_2_name   VARCHAR(255),
    guardian_2_mobile VARCHAR(15),
    user_uuid         UUID         NOT NULL UNIQUE,
    created_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN      NOT NULL        DEFAULT FALSE
);

CREATE INDEX idx_user_uuid ON students (user_uuid);
CREATE INDEX idx_uuid ON students (uuid);