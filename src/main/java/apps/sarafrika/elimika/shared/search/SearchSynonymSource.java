package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Map;

/**
 * A live source of engine synonyms kept outside the index definitions, such as the skills taxonomy.
 * The module that owns the words implements it. An index opts in by naming the source in
 * {@link SearchIndexDefinition#synonymSources()}.
 * <p>
 * The search module merges each named source's {@link #synonyms()} into the definition's own
 * synonyms whenever it applies index settings: on startup, on a rebuild, and when the owning module
 * publishes {@link SearchSynonymsChanged} with this source's {@link #name()}. A change therefore
 * reaches the engine without a rebuild.
 */
public interface SearchSynonymSource {

    /** The skills taxonomy: every skill name and its aliases are synonyms of each other. */
    String SKILLS = "skills";

    /** The name an index definition refers to this source by, e.g. {@link #SKILLS}. */
    String name();

    /**
     * Word to the words that mean the same, lower-cased. Read from the owning module's tables each
     * time settings are applied, so it must be cheap enough to call once per index.
     */
    Map<String, List<String>> synonyms();
}
