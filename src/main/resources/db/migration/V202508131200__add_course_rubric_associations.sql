-- Create course_rubric_associations table to support many-to-many relationship
-- This allows rubrics to be reused across multiple courses

CREATE TABLE course_rubric_associations (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    
    -- Foreign key references
    course_uuid UUID NOT NULL,
    rubric_uuid UUID NOT NULL,
    
    -- Association metadata
    associated_by UUID NOT NULL, -- UUID of instructor who made the association
    association_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Usage tracking
    is_primary_rubric BOOLEAN DEFAULT FALSE, -- Marks the main rubric for the course
    usage_context VARCHAR(100), -- Context like 'midterm', 'final', 'assignment', etc.
    
    -- Audit fields
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    
    -- Constraints
    CONSTRAINT uk_course_rubric_associations_uuid UNIQUE(uuid),
    CONSTRAINT uk_course_rubric_context UNIQUE(course_uuid, rubric_uuid, usage_context),
    
    -- Foreign key constraints (referential integrity will be maintained by application)
    CONSTRAINT fk_course_rubric_associations_rubric 
        FOREIGN KEY (rubric_uuid) REFERENCES assessment_rubrics(uuid) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_course_rubric_associations_course_uuid ON course_rubric_associations(course_uuid);
CREATE INDEX idx_course_rubric_associations_rubric_uuid ON course_rubric_associations(rubric_uuid);
CREATE INDEX idx_course_rubric_associations_associated_by ON course_rubric_associations(associated_by);
CREATE INDEX idx_course_rubric_associations_primary ON course_rubric_associations(course_uuid, is_primary_rubric) WHERE is_primary_rubric = true;

-- Create trigger to update last_modified_date
CREATE TRIGGER course_rubric_associations_updated_at
    BEFORE UPDATE ON course_rubric_associations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE course_rubric_associations IS 'Many-to-many association between courses and rubrics, allowing rubrics to be reused across multiple courses';
COMMENT ON COLUMN course_rubric_associations.course_uuid IS 'Reference to the course that uses this rubric';
COMMENT ON COLUMN course_rubric_associations.rubric_uuid IS 'Reference to the rubric being used by the course';
COMMENT ON COLUMN course_rubric_associations.associated_by IS 'UUID of the instructor who associated the rubric with the course';
COMMENT ON COLUMN course_rubric_associations.is_primary_rubric IS 'Indicates if this is the primary/default rubric for the course';
COMMENT ON COLUMN course_rubric_associations.usage_context IS 'Context of rubric usage (e.g., midterm, final, assignment)';