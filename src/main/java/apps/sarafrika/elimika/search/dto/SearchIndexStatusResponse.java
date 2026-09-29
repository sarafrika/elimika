package apps.sarafrika.elimika.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * One index as the admin endpoints report it: its definition, the sync state recorded in PostgreSQL,
 * and what the engine currently holds.
 */
public record SearchIndexStatusResponse(
        @JsonProperty("index_name") String indexName,
        @JsonProperty("module") String module,
        @JsonProperty("definition_schema_version") int definitionSchemaVersion,
        @JsonProperty("built_schema_version") Integer builtSchemaVersion,
        @JsonProperty("status") String status,
        @JsonProperty("read_enabled") boolean readEnabled,
        @JsonProperty("build_index_name") String buildIndexName,
        @JsonProperty("rebuild_checkpoint_id") Long rebuildCheckpointId,
        @JsonProperty("last_built_at") Instant lastBuiltAt,
        @JsonProperty("last_reconciled_at") Instant lastReconciledAt,
        @JsonProperty("recorded_document_count") Long recordedDocumentCount,
        @JsonProperty("drift") Long drift,
        @JsonProperty("last_error") String lastError,
        @JsonProperty("engine_document_count") Long engineDocumentCount,
        @JsonProperty("engine_indexing") Boolean engineIndexing,
        @JsonProperty("engine_error") String engineError
) {
}
