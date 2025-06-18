-- Create user_domain table
CREATE TABLE user_domain
(
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE
);

-- Insert default domain values
INSERT INTO user_domain (domain_name)
VALUES ('student'),
       ('instructor'),
       ('admin'),
       ('organisation_user');

-- Create index on uuid for performance
CREATE INDEX idx_user_domain_uuid ON user_domain (uuid);

-- Create index on domain_name for performance
CREATE INDEX idx_user_domain_name ON user_domain (domain_name);

-- Create user_domain_mapping pivot table
CREATE TABLE user_domain_mapping
(
    id          BIGSERIAL PRIMARY KEY,
    user_uuid   UUID NOT NULL,
    domain_uuid UUID NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_user_domain_mapping_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_user_domain_mapping_domain
        FOREIGN KEY (domain_uuid) REFERENCES user_domain (uuid) ON DELETE CASCADE,

    -- Ensure unique combination of user and domain
    CONSTRAINT uk_user_domain_mapping UNIQUE (user_uuid, domain_uuid)
);

-- Create indexes for performance
CREATE INDEX idx_user_domain_mapping_user_uuid ON user_domain_mapping (user_uuid);
CREATE INDEX idx_user_domain_mapping_domain_uuid ON user_domain_mapping (domain_uuid);
CREATE INDEX idx_user_domain_mapping_composite ON user_domain_mapping (user_uuid, domain_uuid);