-- Course Bundle Tables Migration
-- Creates tables for course bundling functionality with independent pricing and lifecycle management

-- Create course_bundles table
CREATE TABLE course_bundles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    instructor_uuid UUID NOT NULL,
    description TEXT,
    price DECIMAL(10, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
    active BOOLEAN DEFAULT false,
    validity_days INTEGER,
    discount_percentage DECIMAL(5, 2),
    thumbnail_url VARCHAR(500),
    banner_url VARCHAR(500),
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    
    -- Constraints
    CONSTRAINT course_bundles_uuid_not_null CHECK (uuid IS NOT NULL),
    CONSTRAINT course_bundles_name_not_empty CHECK (name IS NOT NULL AND LENGTH(TRIM(name)) > 0),
    CONSTRAINT course_bundles_instructor_uuid_not_null CHECK (instructor_uuid IS NOT NULL),
    CONSTRAINT course_bundles_price_non_negative CHECK (price IS NULL OR price >= 0),
    CONSTRAINT course_bundles_status_valid CHECK (status IN ('draft', 'in_review', 'published', 'archived')),
    CONSTRAINT course_bundles_validity_days_positive CHECK (validity_days IS NULL OR validity_days > 0),
    CONSTRAINT course_bundles_discount_percentage_valid CHECK (discount_percentage IS NULL OR (discount_percentage >= 0 AND discount_percentage <= 100))
);

-- Create course_bundle_courses table (junction table)
CREATE TABLE course_bundle_courses (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    bundle_uuid UUID NOT NULL,
    course_uuid UUID NOT NULL,
    sequence_order INTEGER,
    is_required BOOLEAN DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    
    -- Constraints
    CONSTRAINT course_bundle_courses_uuid_not_null CHECK (uuid IS NOT NULL),
    CONSTRAINT course_bundle_courses_bundle_uuid_not_null CHECK (bundle_uuid IS NOT NULL),
    CONSTRAINT course_bundle_courses_course_uuid_not_null CHECK (course_uuid IS NOT NULL),
    CONSTRAINT course_bundle_courses_sequence_order_positive CHECK (sequence_order IS NULL OR sequence_order > 0),
    
    -- Unique constraint to prevent duplicate course-bundle associations
    CONSTRAINT course_bundle_courses_unique_bundle_course UNIQUE (bundle_uuid, course_uuid)
);

-- Create indexes for performance
CREATE INDEX idx_course_bundles_instructor_uuid ON course_bundles(instructor_uuid);
CREATE INDEX idx_course_bundles_status ON course_bundles(status);
CREATE INDEX idx_course_bundles_status_active ON course_bundles(status, active);
CREATE INDEX idx_course_bundles_name ON course_bundles(name);
CREATE INDEX idx_course_bundles_created_date ON course_bundles(created_date);

CREATE INDEX idx_course_bundle_courses_bundle_uuid ON course_bundle_courses(bundle_uuid);
CREATE INDEX idx_course_bundle_courses_course_uuid ON course_bundle_courses(course_uuid);
CREATE INDEX idx_course_bundle_courses_bundle_sequence ON course_bundle_courses(bundle_uuid, sequence_order);
CREATE INDEX idx_course_bundle_courses_bundle_required ON course_bundle_courses(bundle_uuid, is_required);

-- Add foreign key constraints (assuming courses table exists with uuid column)
-- Note: These will need to be adjusted based on your actual table structure
-- ALTER TABLE course_bundle_courses 
-- ADD CONSTRAINT fk_course_bundle_courses_bundle 
-- FOREIGN KEY (bundle_uuid) REFERENCES course_bundles(uuid) ON DELETE CASCADE;

-- ALTER TABLE course_bundle_courses 
-- ADD CONSTRAINT fk_course_bundle_courses_course 
-- FOREIGN KEY (course_uuid) REFERENCES courses(uuid) ON DELETE CASCADE;

-- Add comments for documentation
COMMENT ON TABLE course_bundles IS 'Course bundles for packaging multiple courses with independent pricing';
COMMENT ON COLUMN course_bundles.name IS 'Bundle name displayed to students';
COMMENT ON COLUMN course_bundles.instructor_uuid IS 'UUID of the instructor who owns this bundle';
COMMENT ON COLUMN course_bundles.price IS 'Bundle price independent of individual course prices';
COMMENT ON COLUMN course_bundles.status IS 'Bundle lifecycle status: draft, in_review, published, archived';
COMMENT ON COLUMN course_bundles.validity_days IS 'Number of days bundle access remains valid after purchase';
COMMENT ON COLUMN course_bundles.discount_percentage IS 'Marketing discount percentage (informational only)';

COMMENT ON TABLE course_bundle_courses IS 'Junction table linking bundles to courses with sequencing';
COMMENT ON COLUMN course_bundle_courses.sequence_order IS 'Suggested learning order of course within bundle';
COMMENT ON COLUMN course_bundle_courses.is_required IS 'Whether course is required for bundle completion';