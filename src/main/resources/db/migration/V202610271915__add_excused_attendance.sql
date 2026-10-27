-- An excused session is left out of a learner's attendance instead of counting as an absence.
ALTER TABLE class_enrollments
    DROP CONSTRAINT IF EXISTS class_enrollments_status_check;

ALTER TABLE class_enrollments
    ADD CONSTRAINT class_enrollments_status_check
        CHECK (status IN ('RESERVED', 'ENROLLED', 'WAITLISTED', 'ATTENDED', 'ABSENT', 'EXCUSED', 'CANCELLED'));

-- An excused gradebook cell is settled but unscored, so it never drags the course average down.
ALTER TABLE course_assessment_line_item_scores
    ADD COLUMN IF NOT EXISTS excused BOOLEAN NOT NULL DEFAULT false;
