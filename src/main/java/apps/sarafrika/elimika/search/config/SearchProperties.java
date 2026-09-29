package apps.sarafrika.elimika.search.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code search.*} settings. Everything defaults to off: with {@code enabled=false} the application
 * behaves exactly as it does without a search engine.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    /** Master switch for indexing and search. */
    private boolean enabled = false;

    /** Rebuild an index automatically on startup when its definition's schema version moved on. */
    private boolean autoRebuild = false;

    /**
     * Per index, whether reads are routed to search. Indexing runs whenever search is enabled; reads
     * are opted in separately so an index can be built and checked before traffic depends on it.
     */
    private Map<String, Boolean> readEnabled = new HashMap<>();

    /** How often indexes are compared against their sources for drift. */
    private Duration reconcileInterval = Duration.ofHours(1);

    /** How long an indexing request may stay incomplete before it is resubmitted. */
    private Duration resubmitOlderThan = Duration.ofMinutes(5);

    /** Rows loaded per rebuild batch. */
    private int rebuildBatchSize = 500;

    private Meilisearch meilisearch = new Meilisearch();

    public boolean isReadEnabled(String index) {
        return enabled && Boolean.TRUE.equals(readEnabled.getOrDefault(index, false));
    }

    @Getter
    @Setter
    public static class Meilisearch {

        /** Base URL of the engine; reachable only on the internal Docker network. */
        private String host = "http://meilisearch:7700";

        /** A scoped API key - never the master key. */
        private String apiKey = "";

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration readTimeout = Duration.ofSeconds(5);

        /** How long a write waits for its engine task to finish before it counts as failed. */
        private Duration taskWaitTimeout = Duration.ofSeconds(30);
    }
}
