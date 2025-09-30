-- Create course_creators table
CREATE TABLE course_creators
(
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_uuid              UUID                     NOT NULL UNIQUE,
    full_name              VARCHAR(255)             NOT NULL,
    bio                    TEXT,
    professional_headline  VARCHAR(500),
    website                VARCHAR(500),
    admin_verified         BOOLEAN                  NOT NULL        DEFAULT false,
    created_date           TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date           TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by             VARCHAR(255)             NOT NULL,
    updated_by             VARCHAR(255),

    -- Foreign key constraint
    CONSTRAINT fk_course_creator_user
        FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE
);

-- Create performance indexes
CREATE INDEX idx_course_creators_uuid ON course_creators (uuid);
CREATE INDEX idx_course_creators_user_uuid ON course_creators (user_uuid);
CREATE INDEX idx_course_creators_admin_verified ON course_creators (admin_verified);
CREATE INDEX idx_course_creators_created_date ON course_creators (created_date);

-- Comments for clarity
COMMENT ON TABLE course_creators IS 'Stores course creator profiles for users focused on content creation';
COMMENT ON COLUMN course_creators.user_uuid IS 'Reference to the user account';
COMMENT ON COLUMN course_creators.admin_verified IS 'Indicates if the course creator has been verified by an admin';