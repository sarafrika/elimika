package apps.sarafrika.elimika.shared.search;

/**
 * Published by the module that owns a {@link SearchSynonymSource} after its words changed (a skill
 * created, renamed, given new aliases, retired or deleted). The search module re-applies the
 * settings of every index whose definition names {@code source}, without rebuilding it.
 *
 * @param source the {@link SearchSynonymSource#name()} that changed
 */
public record SearchSynonymsChanged(String source) {
}
