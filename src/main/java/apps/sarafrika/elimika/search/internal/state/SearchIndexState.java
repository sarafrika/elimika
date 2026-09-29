package apps.sarafrika.elimika.search.internal.state;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sync and rebuild state of one search index. Deliberately not a {@code BaseEntity}: it is keyed by
 * the index name, and it must not itself trigger search indexing or audit validation.
 */
@Entity
@Table(name = "search_index_state")
@Getter
@Setter
@NoArgsConstructor
public class SearchIndexState {

    @Id
    @Column(name = "index_name")
    private String indexName;

    @Column(name = "schema_version")
    private int schemaVersion;

    @Convert(converter = SearchIndexStatusConverter.class)
    @Column(name = "status")
    private SearchIndexStatus status;

    @Column(name = "build_index_name")
    private String buildIndexName;

    @Column(name = "rebuild_checkpoint_id")
    private Long rebuildCheckpointId;

    @Column(name = "last_built_at")
    private Instant lastBuiltAt;

    @Column(name = "last_reconciled_at")
    private Instant lastReconciledAt;

    @Column(name = "document_count")
    private Long documentCount;

    @Column(name = "drift")
    private Long drift;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    /** A never-built index: version 0, so any definition counts as newer. */
    public static SearchIndexState initial(String indexName) {
        SearchIndexState state = new SearchIndexState();
        state.setIndexName(indexName);
        state.setSchemaVersion(0);
        state.setStatus(SearchIndexStatus.STALE);
        return state;
    }

    @PrePersist
    void onCreate() {
        createdDate = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    void onUpdate() {
        updatedDate = LocalDateTime.now(ZoneOffset.UTC);
    }
}
