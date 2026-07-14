-- Fix certificates.student_uuid foreign key: every other student_uuid column in the schema
-- references students(uuid) and all callers pass student profile UUIDs, but this FK was
-- created against users(uuid), making every certificate insert fail.

ALTER TABLE certificates DROP CONSTRAINT certificates_student_uuid_fkey;
ALTER TABLE certificates ADD CONSTRAINT certificates_student_uuid_fkey
    FOREIGN KEY (student_uuid) REFERENCES students (uuid);

COMMENT ON CONSTRAINT certificates_student_uuid_fkey ON certificates IS 'Certificates are issued to student profiles, consistent with enrollments and reviews.';
