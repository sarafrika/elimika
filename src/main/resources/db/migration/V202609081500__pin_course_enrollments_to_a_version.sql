-- Record which version of a course a learner enrolled on.
--
-- Course content is edited in place: an approved edit is promoted onto the live rows, so a learner
-- who paid for one syllabus can find a different one the next morning. course_version_snapshots
-- already keeps what each approved version contained; this is the other half — the pointer saying
-- which of those versions a given enrolment was sold.
--
-- NULL means "follow the live course", and is deliberately the default:
--   * every enrolment that predates this column was sold against whatever was live at the time,
--     and we have no honest way to say which version that was. Guessing a number would invent a
--     provenance the data does not have.
--   * a course that has never had an edit promoted has no snapshot to point at, so its enrolments
--     have nothing to pin to. They follow live, which is the same content either way.
--
-- Populated at enrolment time by CourseEnrollmentServiceImpl from the course's latest snapshot.
ALTER TABLE course_enrollments
    ADD COLUMN course_version INTEGER;

COMMENT ON COLUMN course_enrollments.course_version IS
    'Version number in course_version_snapshots this enrolment was sold against. NULL follows the live course: either the enrolment predates version pinning, or the course has no promoted version yet.';

CREATE INDEX idx_course_enrollments_course_version
    ON course_enrollments (course_uuid, course_version)
    WHERE course_version IS NOT NULL;
