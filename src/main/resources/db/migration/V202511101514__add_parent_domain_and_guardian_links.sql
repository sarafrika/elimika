-- Adds the parent user domain and guardian/student linkage table

-- Ensure the parent domain exists for guardian users
INSERT INTO user_domain (domain_name)
SELECT 'parent'
WHERE NOT EXISTS (
    SELECT 1 FROM user_domain WHERE domain_name = 'parent'
);

-- Table storing guardian ↔ student relationships
CREATE TABLE student_guardian_links
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    student_uuid        UUID                     NOT NULL REFERENCES students (uuid) ON DELETE CASCADE,
    guardian_user_uuid  UUID                     NOT NULL REFERENCES users (uuid) ON DELETE CASCADE,
    relationship_type   VARCHAR(50)              NOT NULL,
    share_scope         VARCHAR(50)              NOT NULL        DEFAULT 'FULL',
    link_status         VARCHAR(20)              NOT NULL        DEFAULT 'ACTIVE',
    is_primary          BOOLEAN                  NOT NULL        DEFAULT FALSE,
    invited_by          UUID REFERENCES users (uuid),
    linked_date         TIMESTAMP WITH TIME ZONE,
    revoked_date        TIMESTAMP WITH TIME ZONE,
    revoked_by          UUID REFERENCES users (uuid),
    notes               TEXT,
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date        TIMESTAMP WITH TIME ZONE                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    CONSTRAINT uk_student_guardian UNIQUE (student_uuid, guardian_user_uuid),
    CONSTRAINT chk_guardian_share_scope CHECK (share_scope IN ('FULL', 'ACADEMICS', 'ATTENDANCE')),
    CONSTRAINT chk_guardian_status CHECK (link_status IN ('PENDING', 'ACTIVE', 'REVOKED')),
    CONSTRAINT chk_guardian_relationship CHECK (relationship_type IN ('PARENT', 'GUARDIAN', 'SPONSOR'))
);

CREATE INDEX idx_student_guardian_guardian ON student_guardian_links (guardian_user_uuid);
CREATE INDEX idx_student_guardian_student ON student_guardian_links (student_uuid);
CREATE INDEX idx_student_guardian_status ON student_guardian_links (link_status);
