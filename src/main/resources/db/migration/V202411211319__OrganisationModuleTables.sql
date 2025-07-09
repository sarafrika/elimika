CREATE TABLE IF NOT EXISTS organisation
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name         VARCHAR(50) NOT NULL,
    description  VARCHAR,
    active       BOOLEAN     NOT NULL        DEFAULT true,
    code         VARCHAR     NOT NULL UNIQUE,
    domain       VARCHAR(255),
    licence_no   VARCHAR(100),
    keycloak_id  varchar(36),
    slug         varchar(200),
    created_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50) NOT NULL,
    updated_date TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted      BOOLEAN     NOT NULL        DEFAULT FALSE
);

-- Indices remain the same
CREATE UNIQUE INDEX idx_organisation_uuid ON organisation (uuid);
CREATE INDEX idx_organisation_name ON organisation (name);
CREATE INDEX idx_organisation_active ON organisation (active);

-- Creation of Gender Type
CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'PREFER_NOT_TO_SAY');

-- Users Table - FIXED: Changed organisation_id to organisation_uuid
CREATE TABLE IF NOT EXISTS users
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    first_name        VARCHAR(50) NOT NULL,
    middle_name       VARCHAR(50),
    last_name         VARCHAR(50) NOT NULL,
    email             VARCHAR(50) NOT NULL UNIQUE,
    phone_number      VARCHAR(50) UNIQUE,
    gender            gender      NULL,
    dob               DATE,
    active            BOOLEAN     NOT NULL        DEFAULT true,
    keycloak_id       varchar(36),
    created_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN     NOT NULL        DEFAULT FALSE
);

-- Indices for Users Table - FIXED: Updated index name
CREATE UNIQUE INDEX idx_users_uuid ON users (uuid);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone_number ON users (phone_number);
CREATE INDEX idx_users_gender ON users (gender);