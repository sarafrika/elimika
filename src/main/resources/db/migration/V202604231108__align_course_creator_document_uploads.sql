ALTER TABLE course_creator_documents
    ADD COLUMN experience_uuid UUID NULL,
    ADD COLUMN membership_uuid UUID NULL,
    ADD COLUMN file_hash VARCHAR(64),
    ADD COLUMN title VARCHAR(255),
    ADD COLUMN description TEXT,
    ADD COLUMN upload_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN expiry_date DATE NULL,
    ADD CONSTRAINT fk_course_creator_documents_experience FOREIGN KEY (experience_uuid) REFERENCES course_creator_experience (uuid) ON DELETE CASCADE,
    ADD CONSTRAINT fk_course_creator_documents_membership FOREIGN KEY (membership_uuid) REFERENCES course_creator_professional_memberships (uuid) ON DELETE CASCADE;

CREATE INDEX idx_course_creator_documents_experience_uuid ON course_creator_documents (experience_uuid);
CREATE INDEX idx_course_creator_documents_membership_uuid ON course_creator_documents (membership_uuid);
CREATE INDEX idx_course_creator_documents_status ON course_creator_documents (status);
CREATE INDEX idx_course_creator_documents_upload_date ON course_creator_documents (upload_date);
