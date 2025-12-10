-- Tracks bulk invitation uploads by file hash to prevent duplicate processing

CREATE TABLE IF NOT EXISTS bulk_invitation_uploads
(
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organisation_uuid UUID         NOT NULL,
    inviter_uuid      UUID         NOT NULL,
    file_name         VARCHAR(255) NOT NULL,
    file_hash         VARCHAR(128) NOT NULL,
    created_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(50)  NOT NULL,
    updated_by        VARCHAR(50),

    CONSTRAINT fk_bulk_upload_organisation
        FOREIGN KEY (organisation_uuid) REFERENCES organisation (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_bulk_upload_inviter
        FOREIGN KEY (inviter_uuid) REFERENCES users (uuid) ON DELETE RESTRICT,
    CONSTRAINT uq_bulk_upload_org_hash UNIQUE (organisation_uuid, file_hash)
);

CREATE UNIQUE INDEX idx_bulk_invitation_uploads_uuid ON bulk_invitation_uploads (uuid);
CREATE INDEX idx_bulk_invitation_uploads_org ON bulk_invitation_uploads (organisation_uuid);
CREATE INDEX idx_bulk_invitation_uploads_hash ON bulk_invitation_uploads (file_hash);

COMMENT ON TABLE bulk_invitation_uploads IS 'Audit log of bulk invitation uploads keyed by file hash to avoid duplicates per organisation';
COMMENT ON COLUMN bulk_invitation_uploads.uuid IS 'Unique identifier for the bulk upload record';
COMMENT ON COLUMN bulk_invitation_uploads.organisation_uuid IS 'Organisation the upload belongs to';
COMMENT ON COLUMN bulk_invitation_uploads.inviter_uuid IS 'User who uploaded the file';
COMMENT ON COLUMN bulk_invitation_uploads.file_name IS 'Original filename of the upload';
COMMENT ON COLUMN bulk_invitation_uploads.file_hash IS 'SHA-256 hash of the uploaded file contents';
COMMENT ON COLUMN bulk_invitation_uploads.created_date IS 'When the upload was recorded';
COMMENT ON COLUMN bulk_invitation_uploads.updated_date IS 'When the upload record was last updated';
COMMENT ON COLUMN bulk_invitation_uploads.created_by IS 'Identifier of the user who created the upload record';
COMMENT ON COLUMN bulk_invitation_uploads.updated_by IS 'Identifier of the user who last updated the upload record';
