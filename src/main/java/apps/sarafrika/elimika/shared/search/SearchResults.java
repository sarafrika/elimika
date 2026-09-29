package apps.sarafrika.elimika.shared.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Puts hydrated rows back together with the page of hits they came from.
 * <p>
 * A read routed to search loads the hit UUIDs from the database and re-checks them there, so a row
 * the index has not caught up with (deleted, or no longer visible to the caller) can drop out. When
 * that happens the engine's total no longer describes what the caller may see: counted over rows they
 * cannot see, it would report those rows' existence as plainly as returning them. The total is then
 * restated from what is shown, the same rule as {@code EnrollmentVisibilityService#visibleToCaller}.
 */
public final class SearchResults {

    private SearchResults() {
    }

    /** The rows in hit order, dropping any hit without a row. */
    public static <T> List<T> inHitOrder(List<UUID> hitUuids, Collection<T> rows, Function<T, UUID> uuidOf) {
        if (hitUuids == null || hitUuids.isEmpty() || rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, T> byUuid = new LinkedHashMap<>();
        for (T row : rows) {
            if (row != null) {
                byUuid.putIfAbsent(uuidOf.apply(row), row);
            }
        }
        List<T> ordered = new ArrayList<>(hitUuids.size());
        for (UUID uuid : hitUuids) {
            T row = uuid == null ? null : byUuid.get(uuid);
            if (row != null) {
                ordered.add(row);
            }
        }
        return ordered;
    }

    /**
     * The total to report for a page: the engine's when every hit survived hydration, otherwise the
     * number of rows actually shown.
     *
     * @param engineTotal the engine's total for the query and scope
     * @param hitCount    hits on the page the engine returned
     * @param shownCount  rows that survived hydration and re-checks
     */
    public static long total(long engineTotal, int hitCount, int shownCount) {
        return shownCount == hitCount ? engineTotal : shownCount;
    }

    /** The UUIDs of a page's hits, in rank order, without nulls. */
    public static List<UUID> hitUuids(SearchPage page) {
        return page.hits().stream().map(SearchHit::uuid).filter(Objects::nonNull).toList();
    }
}
