-- Who graded the work behind each piece of skill evidence; null for system or unknown grades.
ALTER TABLE learner_skill_evidence
    ADD COLUMN IF NOT EXISTS graded_by_uuid UUID REFERENCES users (uuid) ON DELETE SET NULL;

-- Backfill from the grade revision current at the evidence's time, else the cell's current grader.
UPDATE learner_skill_evidence e
SET graded_by_uuid = COALESCE(
        (SELECT r.graded_by_uuid
         FROM course_assessment_grade_revisions r
         WHERE r.line_item_uuid = e.line_item_uuid
           AND r.enrollment_uuid = e.course_enrollment_uuid
           AND r.graded_by_uuid IS NOT NULL
           AND (r.graded_at IS NULL OR r.graded_at <= (e.graded_at AT TIME ZONE 'UTC') + INTERVAL '1 second')
         ORDER BY r.revision_number DESC
         LIMIT 1),
        (SELECT s.graded_by_uuid
         FROM course_assessment_line_item_scores s
         WHERE s.line_item_uuid = e.line_item_uuid
           AND s.enrollment_uuid = e.course_enrollment_uuid))
WHERE e.graded_by_uuid IS NULL;
