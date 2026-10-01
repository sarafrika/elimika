package apps.sarafrika.elimika.search.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
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

    /**
     * When every index is rebuilt blue/green, refreshing names copied from other modules; UTC.
     * {@code -} disables the nightly rebuild.
     */
    private String fullRebuildCron = "0 30 1 * * *";

    /** Rows loaded per rebuild batch. */
    private int rebuildBatchSize = 500;

    /**
     * How many times the startup check tries to create an index and apply its settings before it
     * gives up on that index (logged at error; the other indexes still go ahead).
     */
    private int startupRetryAttempts = 5;

    /** The pause after the first failed startup attempt; it doubles after every further failure. */
    private Duration startupRetryInitialBackoff = Duration.ofSeconds(1);

    private Meilisearch meilisearch = new Meilisearch();

    /**
     * Keys are matched ignoring case and separators: an environment variable such as
     * {@code SEARCH_READENABLED_MARKETPLACE_JOBS} binds to the key {@code marketplace.jobs}, not
     * {@code marketplace_jobs}, so an exact lookup would never find it.
     */
    public boolean isReadEnabled(String index) {
        if (!enabled || index == null) {
            return false;
        }
        String wanted = normaliseIndexKey(index);
        return readEnabled.entrySet().stream()
                .anyMatch(entry -> normaliseIndexKey(entry.getKey()).equals(wanted)
                        && Boolean.TRUE.equals(entry.getValue()));
    }

    private static String normaliseIndexKey(String key) {
        return key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    public static class Meilisearch {

        /** Base URL of the engine; reachable only on the internal Docker network. */
        private String host = "http://meilisearch:7700";

        /** A scoped API key - never the master key. */
        private String apiKey = "";

        private Duration connectTimeout = Duration.ofSeconds(2);

        /** Read timeout for searches and document writes; kept short so a slow engine fails fast. */
        private Duration readTimeout = Duration.ofSeconds(5);

        /**
         * Read timeout for index administration (create, settings, swap, delete), which the engine
         * may answer slowly while it is busy with a rebuild.
         */
        private Duration adminReadTimeout = Duration.ofSeconds(30);

        /** How long a write waits for its engine task to finish before it counts as failed. */
        private Duration taskWaitTimeout = Duration.ofSeconds(30);
    }
}
