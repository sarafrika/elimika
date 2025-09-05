-- Create class_resources table for storing resources associated with class definitions
-- Resources can include documents, links, files, and other materials that support class delivery

CREATE TABLE class_resources (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Association
    class_definition_uuid UUID NOT NULL,
    
    -- Basic Information
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    
    -- Resource Details
    resource_type    VARCHAR(50),  -- e.g., PRESENTATION, DOCUMENT, VIDEO, LINK, etc.
    resource_url     VARCHAR(1000), -- For external links
    
    -- File Information (if stored in system)
    file_path        VARCHAR(500),
    file_size        BIGINT,
    mime_type        VARCHAR(100),
    
    -- Status
    is_active        BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit Fields
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255) NOT NULL,
    updated_by       VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_class_resources_class_definition ON class_resources(class_definition_uuid);
CREATE INDEX idx_class_resources_type ON class_resources(resource_type);
CREATE INDEX idx_class_resources_active ON class_resources(is_active);

-- Add check constraints
ALTER TABLE class_resources ADD CONSTRAINT chk_file_size_positive 
    CHECK (file_size IS NULL OR file_size > 0);

-- Add comments for documentation
COMMENT ON TABLE class_resources IS 'Resources associated with class definitions including documents, links, and supporting materials';
COMMENT ON COLUMN class_resources.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN class_resources.class_definition_uuid IS 'Reference to the class definition this resource belongs to';
COMMENT ON COLUMN class_resources.title IS 'Display name of the resource';
COMMENT ON COLUMN class_resources.description IS 'Detailed description of the resource content';
COMMENT ON COLUMN class_resources.resource_type IS 'Category of resource (PRESENTATION, DOCUMENT, VIDEO, LINK, etc.)';
COMMENT ON COLUMN class_resources.resource_url IS 'URL if resource is hosted externally';
COMMENT ON COLUMN class_resources.file_path IS 'File system path if resource is stored locally';
COMMENT ON COLUMN class_resources.file_size IS 'File size in bytes if stored locally';
COMMENT ON COLUMN class_resources.mime_type IS 'MIME type of the file if stored locally';
COMMENT ON COLUMN class_resources.is_active IS 'Whether this resource is currently active';