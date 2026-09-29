-- One row per search index: where its sync and rebuild stand. The index contents themselves live in
-- the search engine and are disposable projections of other tables; this row is the only search state
-- kept in PostgreSQL, so a lost engine volume is recovered by a rebuild, not a restore.

CREATE TABLE search_index_state
(
    index_name            VARCHAR(128) PRIMARY KEY,
    -- The document schema version the live index was last built with; 0 = never built.
    schema_version        INTEGER      NOT NULL DEFAULT 0,
    status                VARCHAR(20)  NOT NULL DEFAULT 'STALE',
    -- While REBUILDING: the build index being filled, and the last source row id copied into it, so a
    -- rebuild interrupted by a restart resumes instead of starting over.
    build_index_name      VARCHAR(160),
    rebuild_checkpoint_id BIGINT,
    last_built_at         TIMESTAMPTZ,
    last_reconciled_at    TIMESTAMPTZ,
    document_count        BIGINT,
    -- Engine document count minus the source's indexable count at the last reconciliation.
    drift                 BIGINT,
    last_error            TEXT,
    created_date          TIMESTAMP    NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_date          TIMESTAMP,
    CONSTRAINT chk_search_index_state_status
        CHECK (UPPER(status) IN ('READY', 'REBUILDING', 'FAILED', 'STALE'))
);
