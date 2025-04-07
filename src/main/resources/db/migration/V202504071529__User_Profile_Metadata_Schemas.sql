CREATE TABLE IF NOT EXISTS training_experience
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE,
    organisation_name VARCHAR(255) NOT NULL,
    job_title         VARCHAR(255) NOT NULL,
    start_date        DATE         NOT NULL,
    end_date          DATE,
    work_description  TEXT,
    user_uuid         UUID         NOT NULL,
    created_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50)
);

CREATE INDEX idx_training_experience_start_date ON training_experience (start_date);
CREATE INDEX idx_training_experience_end_date ON training_experience (end_date);
CREATE INDEX idx_training_experience_user_uuid ON training_experience (user_uuid);

CREATE TABLE IF NOT EXISTS professional_bodies
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL UNIQUE,
    body_name     VARCHAR(255) NOT NULL,
    membership_no VARCHAR(255) NOT NULL,
    member_since  DATE,
    user_uuid     UUID         NOT NULL,
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50)  NOT NULL,
    updated_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50)
);

CREATE INDEX idx_professional_bodies_member_since ON professional_bodies (member_since);
CREATE INDEX idx_professional_bodies_user_uuid ON professional_bodies (user_uuid);

ALTER TABLE users
    ADD COLUMN bio                   TEXT,
    ADD COLUMN website               VARCHAR(255),
    ADD COLUMN professional_headline VARCHAR(255),
    ADD COLUMN lat                   DECIMAL(10, 8),
    ADD COLUMN long                  DECIMAL(10, 8);

CREATE TABLE IF NOT EXISTS user_certifications
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    issued_date     DATE         NOT NULL,
    issued_by       VARCHAR(50)  NOT NULL,
    certificate_url VARCHAR(255) NOT NULL,
    user_uuid       UUID         NOT NULL,
    created_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)  NOT NULL,
    updated_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_user_certifications_issued_date ON user_certifications (issued_date);
CREATE INDEX idx_user_certifications_user_uuid ON user_certifications (user_uuid);