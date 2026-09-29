package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Optional;

/**
 * Puts one index into global search ({@code GET /api/v1/search}). Implemented once per index by the
 * module that owns it, next to the index's scope factory, and discovered by the search module, which
 * runs the query but knows nothing about the domain.
 * <p>
 * The provider answers two questions for the current caller: may they see this type at all, and
 * within which boundary. The boundary is the same rule the owning module applies to its own listing.
 * Global search does not hydrate hits from the database - {@link #toHit} builds the result straight
 * from the stored document - so the scope is the only thing between the caller and a document.
 */
public interface GlobalSearchProvider {

    /**
     * The result type as the API names it: {@code courses}, {@code programs}, {@code classes},
     * {@code marketplace_jobs}, {@code instructors}, {@code organisations}, {@code people} or
     * {@code rubrics}.
     */
    String type();

    /** The index definition: its name, and the filterable and sortable allow-lists. */
    SearchIndexDefinition definition();

    /** The index this type is served from. */
    default String index() {
        return definition().name();
    }

    /**
     * The current caller's boundary over this index, or empty when they may not see this type in
     * global search at all (the type is then skipped silently). Anonymous callers reach this too, so
     * implementations must fail closed when there is no caller.
     */
    Optional<SearchScope> scopeForCurrentCaller();

    /**
     * The searchable attributes the current caller's query text may match, or {@code null} for all of
     * them. Lets a type narrow what a caller can probe, e.g. names but never email addresses.
     */
    default List<String> searchOnForCurrentCaller() {
        return null;
    }

    /** The lightweight result for one hit, from stored document fields only - no database query. */
    GlobalSearchHit toHit(SearchHit hit);
}
