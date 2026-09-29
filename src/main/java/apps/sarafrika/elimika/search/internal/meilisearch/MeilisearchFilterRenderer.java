package apps.sarafrika.elimika.search.internal.meilisearch;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchSort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders {@link SearchFilter}s into Meilisearch filter expressions.
 * <p>
 * This is the only place a filter becomes a string, and it is written so that no value can change the
 * shape of the expression:
 * <ul>
 *     <li>every value is rendered as a double-quoted string with {@code \} and {@code "} escaped,
 *     numbers included - the engine compares quoted numbers numerically;</li>
 *     <li>attribute names are checked against {@code [A-Za-z0-9_.]+} and rejected otherwise;</li>
 *     <li>every compound operand is parenthesised, so precedence never depends on the input.</li>
 * </ul>
 */
public final class MeilisearchFilterRenderer {

    private static final Pattern ATTRIBUTE = Pattern.compile("[A-Za-z0-9_.]+");

    private MeilisearchFilterRenderer() {
    }

    /** ANDs the scope's boundary with the caller's filter and renders the result; null when neither is set. */
    public static String render(SearchFilter scopeFilter, SearchFilter requestFilter) {
        SearchFilter combined = SearchFilter.and(scopeFilter, requestFilter);
        return combined == null ? null : render(combined);
    }

    public static String render(SearchFilter filter) {
        return switch (filter) {
            case SearchFilter.Eq eq -> attribute(eq.attribute()) + " = " + value(eq.value());
            case SearchFilter.In in -> attribute(in.attribute()) + " IN ["
                    + in.values().stream().map(MeilisearchFilterRenderer::value).collect(Collectors.joining(", "))
                    + "]";
            case SearchFilter.Range range -> renderRange(range);
            case SearchFilter.IsNull isNull -> attribute(isNull.attribute()) + " IS NULL";
            case SearchFilter.Not not -> "NOT (" + render(not.filter()) + ")";
            case SearchFilter.And and -> join(and.filters(), " AND ");
            case SearchFilter.Or or -> join(or.filters(), " OR ");
        };
    }

    public static List<String> renderSort(List<SearchSort> sorts) {
        return sorts.stream()
                .map(sort -> attribute(sort.field()) + ":" + sort.direction().name().toLowerCase(Locale.ROOT))
                .toList();
    }

    /** Validates an attribute name used outside a filter, e.g. in facets or attributesToSearchOn. */
    public static String attribute(String name) {
        if (name == null || !ATTRIBUTE.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid search attribute name");
        }
        return name;
    }

    private static String renderRange(SearchFilter.Range range) {
        String attribute = attribute(range.attribute());
        List<String> parts = new ArrayList<>(4);
        if (range.gte() != null) {
            parts.add(attribute + " >= " + value(range.gte()));
        }
        if (range.gt() != null) {
            parts.add(attribute + " > " + value(range.gt()));
        }
        if (range.lte() != null) {
            parts.add(attribute + " <= " + value(range.lte()));
        }
        if (range.lt() != null) {
            parts.add(attribute + " < " + value(range.lt()));
        }
        return parts.size() == 1 ? parts.getFirst() : "(" + String.join(" AND ", parts) + ")";
    }

    private static String join(List<SearchFilter> filters, String operator) {
        return filters.stream()
                .map(filter -> "(" + render(filter) + ")")
                .collect(Collectors.joining(operator));
    }

    static String value(Object value) {
        String text = switch (value) {
            case BigDecimal decimal -> decimal.toPlainString();
            case Double d -> BigDecimal.valueOf(d).toPlainString();
            case Float f -> new BigDecimal(f.toString()).toPlainString();
            default -> String.valueOf(value);
        };
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
