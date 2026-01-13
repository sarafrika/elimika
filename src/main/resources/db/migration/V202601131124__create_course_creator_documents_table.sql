-- Capture supporting documents for course creators
CREATE TABLE course_creator_documents
(
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    course_creator_uuid UUID                     NOT NULL,
    document_type_uuid  UUID                     NOT NULL,

    -- Reference to specific records (nullable - documents can be general)
    education_uuid      UUID                     NULL,

    -- File information
    original_filename   VARCHAR(255)             NOT NULL,
    stored_filename     VARCHAR(255)             NOT NULL UNIQUE,
    file_path           VARCHAR(500)             NOT NULL,
    file_size_bytes     BIGINT                   NOT NULL,
    mime_type           VARCHAR(100)             NOT NULL,

    created_date        TIMESTAMP WITH TIME ZONE NOT NULL        DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),

    CONSTRAINT fk_course_creator_documents_creator FOREIGN KEY (course_creator_uuid) REFERENCES course_creators (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_course_creator_documents_document_type FOREIGN KEY (document_type_uuid) REFERENCES document_types (uuid),
    CONSTRAINT fk_course_creator_documents_education FOREIGN KEY (education_uuid) REFERENCES course_creator_education (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_course_creator_documents_creator_uuid ON course_creator_documents (course_creator_uuid);
CREATE INDEX idx_course_creator_documents_uuid ON course_creator_documents (uuid);
CREATE INDEX idx_course_creator_documents_type_uuid ON course_creator_documents (document_type_uuid);
CREATE INDEX idx_course_creator_documents_education_uuid ON course_creator_documents (education_uuid);
