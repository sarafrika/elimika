CREATE TABLE IF NOT EXISTS user_group
(
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID        NOT NULL UNIQUE,
    organisation_id BIGINT      NOT NULL,
    name            VARCHAR(50) NOT NULL UNIQUE,
    active          BOOLEAN     NOT NULL DEFAULT true,
    created_date    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    updated_date    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT user_group_organisation_fk FOREIGN KEY (organisation_id)
        REFERENCES organisation (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Indices for User Group Table
CREATE UNIQUE INDEX idx_user_group_uuid ON user_group (uuid);
CREATE INDEX idx_user_group_name ON user_group (name);
CREATE INDEX idx_user_group_active ON user_group (active);
CREATE INDEX idx_user_group_organisation_id ON user_group (organisation_id);

CREATE TABLE IF NOT EXISTS user_group_membership
(
    user_id  BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    CONSTRAINT membership_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT membership_group_fk
        FOREIGN KEY (group_id) REFERENCES user_group (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Indices for User Group Membership Table
CREATE INDEX idx_user_group_membership_user_id ON user_group_membership (user_id);
CREATE INDEX idx_user_group_membership_group_id ON user_group_membership (group_id);

CREATE TABLE IF NOT EXISTS user_group_role
(
    group_id BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (group_id, role_id),
    CONSTRAINT group_role_group_fk
        FOREIGN KEY (group_id) REFERENCES user_group (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT group_role_role_fk
        FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Indices for User Group Role Table
CREATE INDEX idx_user_group_role_group_id ON user_group_role (group_id);
CREATE INDEX idx_user_group_role_role_id ON user_group_role (role_id);
