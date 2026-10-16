-- What was actually taught in a session, recorded by its instructor (workbook: lesson title, practical, theory, remarks).
ALTER TABLE scheduled_instances
    ADD COLUMN topic_covered      TEXT,
    ADD COLUMN instructor_remarks TEXT;

-- Per-learner attendance detail (workbook attendance sheets: instrument condition and a comment per session).
ALTER TABLE class_enrollments
    ADD COLUMN instrument_condition VARCHAR(20),
    ADD COLUMN attendance_comment   TEXT,
    ADD CONSTRAINT chk_class_enrollments_instrument_condition
        CHECK (instrument_condition IS NULL OR UPPER(instrument_condition) IN ('GOOD', 'NEEDS_REPAIR', 'NO_EQUIPMENT'));
