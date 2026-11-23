-- Spring Modulith 2.0 tracks publication status and retry attempts on each event.
ALTER TABLE event_publication
    ADD COLUMN IF NOT EXISTS status                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS completion_attempts    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_resubmission_date TIMESTAMP WITH TIME ZONE;

UPDATE event_publication
SET status = CASE WHEN completion_date IS NOT NULL THEN 'COMPLETED' ELSE 'PUBLISHED' END
WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS event_publication_by_status_idx ON event_publication (status);
