package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Everything the engine needs to know to build an index, owned by the module that defines the
 * document.
 * <p>
 * Bump {@code schemaVersion} whenever the document shape or settings change in a way that existing
 * documents no longer satisfy (a new filterable attribute, a renamed field). On startup an index
 * whose stored version is lower is marked stale and, when {@code search.auto-rebuild} is on,
 * rebuilt blue/green.
 *
 * @param name                   the index uid, lowercase with underscores
 * @param primaryKey             the document key, {@code uuid} unless there is a strong reason
 * @param schemaVersion          positive, monotonically increasing
 * @param searchableAttributes   in priority order; empty means every attribute
 * @param filterableAttributes   the allow-list for filters, including every attribute a scope uses
 * @param sortableAttributes     the allow-list for sorting
 * @param displayedAttributes    returned attributes; empty means every attribute
 * @param rankingRules           custom ranking rules; empty keeps the engine's defaults
 * @param synonyms               word to synonyms
 * @param stopWords              words ignored in queries
 * @param typoDisabledAttributes attributes matched exactly (codes, emails)
 * @param maxTotalHits           the most hits a query can page through; 0 means 1000
 */
public record SearchIndexDefinition(
        String name,
        String primaryKey,
        int schemaVersion,
        List<String> searchableAttributes,
        List<String> filterableAttributes,
        List<String> sortableAttributes,
        List<String> displayedAttributes,
        List<String> rankingRules,
        Map<String, List<String>> synonyms,
        List<String> stopWords,
        List<String> typoDisabledAttributes,
        int maxTotalHits
) {

    public static final String DEFAULT_PRIMARY_KEY = "uuid";
    public static final int DEFAULT_MAX_TOTAL_HITS = 1000;
    private static final Pattern INDEX_NAME = Pattern.compile("[a-z0-9_]+");

    public SearchIndexDefinition {
        Objects.requireNonNull(name, "name");
        if (!INDEX_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Index names are lowercase letters, digits and underscores: " + name);
        }
        if (name.contains("__")) {
            throw new IllegalArgumentException("Index names cannot contain '__', which marks build indexes: " + name);
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be 1 or greater");
        }
        primaryKey = primaryKey == null || primaryKey.isBlank() ? DEFAULT_PRIMARY_KEY : primaryKey;
        searchableAttributes = copy(searchableAttributes);
        filterableAttributes = copy(filterableAttributes);
        sortableAttributes = copy(sortableAttributes);
        displayedAttributes = copy(displayedAttributes);
        rankingRules = copy(rankingRules);
        synonyms = synonyms == null ? Map.of() : Map.copyOf(synonyms);
        stopWords = copy(stopWords);
        typoDisabledAttributes = copy(typoDisabledAttributes);
        maxTotalHits = maxTotalHits <= 0 ? DEFAULT_MAX_TOTAL_HITS : maxTotalHits;
    }

    /**
     * The common case: a {@code uuid}-keyed index with engine defaults for everything but the three
     * attribute lists. Refine with the {@code with*} methods.
     */
    public static SearchIndexDefinition of(
            String name,
            int schemaVersion,
            List<String> searchableAttributes,
            List<String> filterableAttributes,
            List<String> sortableAttributes
    ) {
        return new SearchIndexDefinition(name, DEFAULT_PRIMARY_KEY, schemaVersion, searchableAttributes,
                filterableAttributes, sortableAttributes, List.of(), List.of(), Map.of(), List.of(), List.of(), 0);
    }

    public SearchIndexDefinition withDisplayedAttributes(List<String> attributes) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, attributes, rankingRules, synonyms, stopWords, typoDisabledAttributes, maxTotalHits);
    }

    public SearchIndexDefinition withRankingRules(List<String> rules) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, displayedAttributes, rules, synonyms, stopWords, typoDisabledAttributes, maxTotalHits);
    }

    public SearchIndexDefinition withSynonyms(Map<String, List<String>> words) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, displayedAttributes, rankingRules, words, stopWords, typoDisabledAttributes, maxTotalHits);
    }

    public SearchIndexDefinition withStopWords(List<String> words) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, displayedAttributes, rankingRules, synonyms, words, typoDisabledAttributes, maxTotalHits);
    }

    public SearchIndexDefinition withTypoDisabledAttributes(List<String> attributes) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, displayedAttributes, rankingRules, synonyms, stopWords, attributes, maxTotalHits);
    }

    public SearchIndexDefinition withMaxTotalHits(int hits) {
        return new SearchIndexDefinition(name, primaryKey, schemaVersion, searchableAttributes, filterableAttributes,
                sortableAttributes, displayedAttributes, rankingRules, synonyms, stopWords, typoDisabledAttributes, hits);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
