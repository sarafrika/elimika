-- Fix case-insensitive constraints for course_training_applications
-- Makes the CHECK constraints case-insensitive by using UPPER() function

-- Drop existing constraints
ALTER TABLE course_training_applications
    DROP CONSTRAINT IF EXISTS chk_course_training_applications_applicant_type;

ALTER TABLE course_training_applications
    DROP CONSTRAINT IF EXISTS chk_course_training_applications_status;

-- Recreate constraints with case-insensitive comparison using UPPER()
ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_applications_applicant_type
        CHECK (UPPER(applicant_type) IN ('INSTRUCTOR', 'ORGANISATION'));

ALTER TABLE course_training_applications
    ADD CONSTRAINT chk_course_training_applications_status
        CHECK (UPPER(status) IN ('PENDING', 'APPROVED', 'REJECTED', 'REVOKED'));