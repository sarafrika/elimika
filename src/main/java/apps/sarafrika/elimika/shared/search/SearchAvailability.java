package apps.sarafrika.elimika.shared.search;

/**
 * Lets an owning module decide whether to route a read (a {@code q} parameter) to search or to keep
 * serving it from the database. Reads are switched on per index, independently of indexing, so an
 * index can be built and checked before any traffic depends on it.
 */
public interface SearchAvailability {

    /** Whether search is switched on at all (indexing runs). */
    boolean isEnabled();

    /** Whether reads for {@code index} should go to search: search is on and the index is opted in. */
    boolean isReadEnabled(String index);
}
