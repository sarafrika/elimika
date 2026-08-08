ALTER TABLE instructor_obligations
    ADD COLUMN IF NOT EXISTS session_date DATE;

COMMENT ON COLUMN instructor_obligations.session_date
    IS 'Calendar date the session ran. A per-day class is sold as one day however many sessions it holds, so this is what stops a second session on the same date accruing a second day of pay.';

CREATE INDEX IF NOT EXISTS idx_instructor_obligations_day
    ON instructor_obligations (class_definition_uuid, instructor_uuid, session_date);
