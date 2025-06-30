- V1.8__Create_document_status_enum.sql
CREATE TYPE document_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED');

-- V1.9__Create_instructor_documents_table.sql
CREATE TABLE instructor_documents
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    instructor_uuid    UUID         NOT NULL,
    document_type_uuid UUID         NOT NULL,

    -- Reference to specific records (nullable - documents can be general)
    education_uuid     BIGINT       NULL,
    experience_uuid    BIGINT       NULL,
    membership_uuid    BIGINT       NULL,

    -- File information
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(255) NOT NULL UNIQUE,
    file_path          VARCHAR(500) NOT NULL,
    file_size_bytes    BIGINT       NOT NULL,
    mime_type          VARCHAR(100) NOT NULL,
    file_hash          VARCHAR(64),

    -- Metadata
    title              VARCHAR(255),
    description        TEXT,
    upload_date        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_verified        BOOLEAN               DEFAULT FALSE,
    verified_by        VARCHAR(255) NULL,
    verified_at        TIMESTAMP    NULL,
    verification_notes TEXT,

    -- Status
    status             document_status_enum  DEFAULT 'PENDING',
    expiry_date        DATE         NULL,

    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255),

    CONSTRAINT fk_instructor_documents_instructor
        FOREIGN KEY (instructor_uuid) REFERENCES instructors (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_instructor_documents_document_type
        FOREIGN KEY (document_type_uuid) REFERENCES document_types (uuid),
    CONSTRAINT fk_instructor_documents_education
        FOREIGN KEY (education_uuid) REFERENCES instructor_education (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_instructor_documents_experience
        FOREIGN KEY (experience_uuid) REFERENCES instructor_experience (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_instructor_documents_membership
        FOREIGN KEY (membership_uuid) REFERENCES instructor_professional_memberships (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_instructor_documents_instructor_id ON instructor_documents (instructor_uuid);
CREATE INDEX idx_instructor_documents_uuid ON instructor_documents (uuid);
CREATE INDEX idx_instructor_documents_type ON instructor_documents (document_type_uuid);
CREATE INDEX idx_instructor_documents_status ON instructor_documents (status);
CREATE INDEX idx_instructor_documents_verified ON instructor_documents (is_verified);
CREATE INDEX idx_instructor_documents_hash ON instructor_documents (file_hash);