-- Organisation Table remains the same
CREATE TABLE IF NOT EXISTS organisation
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name         VARCHAR(50)  NOT NULL,
    description  VARCHAR,
    active       BOOLEAN      NOT NULL        DEFAULT true,
    code         VARCHAR      NOT NULL UNIQUE,
    domain       VARCHAR(255),
    keycloak_id  varchar(36),
    slug         varchar(200),
    created_date TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50)  NOT NULL,
    updated_date TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted      BOOLEAN      NOT NULL        DEFAULT FALSE
);

-- Indices remain the same
CREATE UNIQUE INDEX idx_organisation_uuid ON organisation (uuid);
CREATE INDEX idx_organisation_name ON organisation (name);
CREATE INDEX idx_organisation_active ON organisation (active);

-- Creation of Gender Type
CREATE TYPE gender AS ENUM ('MALE', 'FEMALE', 'PREFER_NOT_TO_SAY');

-- Users Table - Modified organisation_id to be nullable
CREATE TABLE IF NOT EXISTS users
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    first_name      VARCHAR(50) NOT NULL,
    middle_name     VARCHAR(50),
    last_name       VARCHAR(50) NOT NULL,
    email           VARCHAR(50) NOT NULL UNIQUE,
    phone_number    VARCHAR(50) UNIQUE,
    gender          gender,
    dob             DATE,
    active          BOOLEAN     NOT NULL        DEFAULT true,
    organisation_id BIGINT,
    keycloak_id     varchar(36),
    created_date    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_date    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted         BOOLEAN     NOT NULL        DEFAULT FALSE,
    CONSTRAINT user_organisation_fk
        FOREIGN KEY (organisation_id)
            REFERENCES organisation (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

-- Indices for Users Table
CREATE UNIQUE INDEX idx_users_uuid ON users (uuid);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_organisation_id ON users (organisation_id);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone_number ON users (phone_number);
CREATE INDEX idx_users_gender ON users (gender);

-- Step 2: Create the permissions table (if it doesn't already exist)
DROP TABLE IF EXISTS permissions CASCADE;
CREATE TABLE permissions
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            uuid UNIQUE           DEFAULT gen_random_uuid(),
    module_name     VARCHAR(255) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    keycloak_id     uuid,
    created_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)  NOT NULL,
    updated_date    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (module_name, permission_name)
);

-- Roles Table
CREATE TABLE role
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID        NOT NULL,
    name              VARCHAR(50) NOT NULL UNIQUE,
    description       VARCHAR(255),
    active            BOOLEAN     NOT NULL        DEFAULT true,
    created_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN     NOT NULL        DEFAULT FALSE
);

-- Role-Permission Mapping Table (Many-to-Many Relationship)
CREATE TABLE IF NOT EXISTS role_permissions
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Indices for Roles Table
CREATE UNIQUE INDEX idx_role_uid ON role (uuid);
CREATE INDEX idx_role_name ON role (name);
CREATE INDEX idx_role_active ON role (active);
CREATE INDEX idx_role_organisation_uid ON role (organisation_uuid);

-- User Role Association Table
CREATE TABLE IF NOT EXISTS user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT role_fk FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Indices for User Role Association Table
CREATE INDEX idx_user_role_user_id ON user_role (user_id);
CREATE INDEX idx_user_role_role_id ON user_role (role_id);