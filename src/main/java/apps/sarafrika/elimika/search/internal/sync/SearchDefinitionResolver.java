package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Turns a module's static {@link SearchIndexDefinition} into the settings actually applied to the
 * engine, by merging in the live synonyms of every {@link SearchSynonymSource} the definition names.
 * <p>
 * Every settings write goes through here: the startup check, blue/green rebuilds, and the refresh on
 * {@code SearchSynonymsChanged}. A source that is missing or fails is logged and skipped, so a
 * taxonomy problem never blocks an index from being created; the next refresh fills the words in.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchDefinitionResolver {

    private final ObjectProvider<SearchSynonymSource> synonymSources;

    public SearchDefinitionResolver(ObjectProvider<SearchSynonymSource> synonymSources) {
        this.synonymSources = synonymSources;
    }

    /** The definition with its named synonym sources merged in. */
    public SearchIndexDefinition effective(SearchIndexDefinition definition) {
        if (definition.synonymSources().isEmpty()) {
            return definition;
        }
        Map<String, SearchSynonymSource> byName = synonymSources.orderedStream()
                .collect(Collectors.toMap(SearchSynonymSource::name, source -> source, (first, second) -> first));
        SearchIndexDefinition effective = definition;
        for (String name : definition.synonymSources()) {
            SearchSynonymSource source = byName.get(name);
            if (source == null) {
                log.warn("Search index {} names synonym source '{}', which no module provides", definition.name(), name);
                continue;
            }
            try {
                effective = effective.mergeSynonyms(source.synonyms());
            } catch (RuntimeException ex) {
                log.warn("Could not read synonym source '{}' for search index {}: {}",
                        name, definition.name(), ex.getMessage());
            }
        }
        return effective;
    }

    /** Whether the definition takes synonyms from {@code source}. */
    public static boolean uses(SearchIndexDefinition definition, String source) {
        return definition.synonymSources().stream().anyMatch(name -> Objects.equals(name, source));
    }
}
