-- V202506301846__Create_document_types_table.sql
CREATE TABLE document_types
(
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name               VARCHAR(100) NOT NULL UNIQUE,
    description        TEXT,
    max_file_size_mb   INTEGER               DEFAULT 10,
    allowed_extensions JSONB,
    is_required        BOOLEAN               DEFAULT FALSE,
    created_date       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date       TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255)
);

CREATE INDEX idx_document_types_uuid ON document_types (uuid);
CREATE INDEX idx_document_types_name ON document_types (name);

INSERT INTO document_types (uuid, name, description, allowed_extensions, is_required, created_date, created_by)
VALUES (gen_random_uuid(), 'CERTIFICATE', 'Educational certificates and diplomas', '[
  "pdf",
  "jpg",
  "jpeg",
  "png"
]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'TRANSCRIPT', 'Academic transcripts and grade reports', '[
         "pdf",
         "jpg",
         "jpeg",
         "png"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'CV_RESUME', 'Curriculum Vitae or Resume', '[
         "pdf",
         "doc",
         "docx"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'PORTFOLIO', 'Work portfolio and samples', '[
         "pdf",
         "jpg",
         "jpeg",
         "png",
         "zip"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'PROFESSIONAL_CERT', 'Professional certifications and licenses', '[
         "pdf",
         "jpg",
         "jpeg",
         "png"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'MEMBERSHIP_CERT', 'Professional membership certificates', '[
         "pdf",
         "jpg",
         "jpeg",
         "png"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'EXPERIENCE_LETTER', 'Employment verification letters', '[
         "pdf",
         "jpg",
         "jpeg",
         "png",
         "doc",
         "docx"
       ]'::jsonb, false, CURRENT_TIMESTAMP, 'SYSTEM'),
       (gen_random_uuid(), 'ID_DOCUMENT', 'Government issued ID documents', '[
         "pdf",
         "jpg",
         "jpeg",
         "png"
       ]'::jsonb, true, CURRENT_TIMESTAMP, 'SYSTEM');