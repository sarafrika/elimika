-- Scope the document type catalogue so each onboarding flow asks for its own paperwork.
-- Existing rows served instructor and course-creator credential uploads only; without a
-- discriminator the organisation form was rendering the required ID_DOCUMENT row and asking
-- an institution for a personal government ID.
ALTER TABLE document_types
    ADD COLUMN applies_to VARCHAR(50);

ALTER TABLE document_types
    ADD COLUMN requires_expiry BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE document_types
SET applies_to = 'CREDENTIAL'
WHERE applies_to IS NULL;

CREATE INDEX idx_document_types_applies_to ON document_types (applies_to);

INSERT INTO document_types (uuid, name, description, allowed_extensions, is_required, applies_to,
                            requires_expiry, created_date, created_by)
VALUES (gen_random_uuid(), 'CERTIFICATE_OF_REGISTRATION',
        'Official certificate of incorporation or business registration issued to the organisation.',
        '["pdf", "jpg", "jpeg", "png"]'::jsonb, TRUE, 'ORGANISATION', FALSE, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'LICENCE_OR_ACCREDITATION',
        'Training licence, accreditation letter, or other regulator approval.',
        '["pdf", "jpg", "jpeg", "png"]'::jsonb, FALSE, 'ORGANISATION', FALSE, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'AUTHORISING_REPRESENTATIVE_LETTER',
        'Letter or board resolution confirming the submitter may register this organisation.',
        '["pdf", "jpg", "jpeg", "png"]'::jsonb, FALSE, 'ORGANISATION', FALSE, CURRENT_TIMESTAMP, 'SYSTEM');

CREATE TABLE organisation_documents
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    organisation_uuid  UUID         NOT NULL,
    document_type_uuid UUID         NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(500) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size_bytes    BIGINT,
    mime_type          VARCHAR(100),
    file_hash          VARCHAR(255),
    title              VARCHAR(255),
    description        TEXT,
    upload_date        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    is_verified        BOOLEAN               DEFAULT FALSE,
    verified_by        VARCHAR(255),
    verified_at        TIMESTAMP,
    verification_notes TEXT,
    status             VARCHAR(50)           DEFAULT 'PENDING',
    expiry_date        DATE,
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255)
);

CREATE INDEX idx_organisation_documents_uuid ON organisation_documents (uuid);
CREATE INDEX idx_organisation_documents_organisation ON organisation_documents (organisation_uuid);
CREATE INDEX idx_organisation_documents_type ON organisation_documents (document_type_uuid);
