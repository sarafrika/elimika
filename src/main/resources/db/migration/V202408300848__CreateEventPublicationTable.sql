CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID                     NOT NULL,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    listener_id      TEXT                     NOT NULL,
    event_type       TEXT                     NOT NULL,
    serialized_event TEXT                     NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    created_date     TIMESTAMP                NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50)              NOT NULL,
    updated_date     TIMESTAMP                NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(50),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);