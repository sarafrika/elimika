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
    gender            gender NULL,
    dob               DATE,
    active            BOOLEAN     NOT NULL        DEFAULT true,
    organisation_uuid UUID,  -- CHANGED: from organisation_id to organisation_uuid
    keycloak_id       varchar(36),
    created_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50) NOT NULL,
    updated_date      TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    deleted           BOOLEAN     NOT NULL        DEFAULT FALSE,
    CONSTRAINT user_organisation_fk
        FOREIGN KEY (organisation_uuid)  -- CHANGED: now references uuid
            REFERENCES organisation (uuid)
            ON DELETE CASCADE
            ON UPDATE CASCADE
);

-- Indices for Users Table - FIXED: Updated index name
CREATE UNIQUE INDEX idx_users_uuid ON users (uuid);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_organisation_uuid ON users (organisation_uuid);  -- CHANGED: from organisation_id to organisation_uuid
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

-- Roles Table - Already uses UUID for organisation reference
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
    deleted           BOOLEAN     NOT NULL        DEFAULT FALSE,

    CONSTRAINT fk_role_organisation_uuid
        FOREIGN KEY (organisation_uuid) REFERENCES organisation(uuid) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Role-Permission Mapping Table - FIXED: Use UUIDs instead of IDs
CREATE TABLE IF NOT EXISTS role_permissions
(
    role_uuid       UUID NOT NULL,        -- CHANGED: from role_id to role_uuid
    permission_uuid UUID NOT NULL,        -- CHANGED: from permission_id to permission_uuid
    PRIMARY KEY (role_uuid, permission_uuid),
    FOREIGN KEY (role_uuid) REFERENCES role (uuid) ON DELETE CASCADE,           -- CHANGED: now references uuid
    FOREIGN KEY (permission_uuid) REFERENCES permissions (uuid) ON DELETE CASCADE  -- CHANGED: now references uuid
);

-- Indices for Roles Table
CREATE UNIQUE INDEX idx_role_uid ON role (uuid);
CREATE INDEX idx_role_name ON role (name);
CREATE INDEX idx_role_active ON role (active);
CREATE INDEX idx_role_organisation_uid ON role (organisation_uuid);

-- User Role Association Table - FIXED: Use UUIDs instead of IDs
CREATE TABLE IF NOT EXISTS user_role
(
    user_uuid UUID NOT NULL,        -- CHANGED: from user_id to user_uuid
    role_uuid UUID NOT NULL,        -- CHANGED: from role_id to role_uuid
    PRIMARY KEY (user_uuid, role_uuid),
    CONSTRAINT user_fk FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE ON UPDATE CASCADE,    -- CHANGED: now references uuid
    CONSTRAINT role_fk FOREIGN KEY (role_uuid) REFERENCES role (uuid) ON DELETE CASCADE ON UPDATE CASCADE     -- CHANGED: now references uuid
);

-- Indices for User Role Association Table - FIXED: Updated index names
CREATE INDEX idx_user_role_user_uuid ON user_role (user_uuid);    -- CHANGED: from user_id to user_uuid
CREATE INDEX idx_user_role_role_uuid ON user_role (role_uuid);    -- CHANGED: from role_id to role_uuid