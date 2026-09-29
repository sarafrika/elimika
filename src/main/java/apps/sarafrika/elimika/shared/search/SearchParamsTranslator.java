package apps.sarafrika.elimika.shared.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Translates the {@code searchParams} vocabulary the list endpoints already accept into search
 * filters and sorts, so an endpoint can route a request to search without a second query language.
 * <p>
 * Keys are {@code field} or {@code field_op} with op one of {@code eq, noteq, in, notin, gt, gte, lt,
 * lte, between}; {@code in}/{@code notin} take comma-separated values and {@code between} takes
 * {@code from,to}. Fields may be written in snake_case or camelCase and are normalised to the index's
 * attribute names.
 * <p>
 * Default-deny, like {@code GenericSpecificationBuilder}: every key must name one of the definition's
 * filterable attributes, and every sort field one of its sortable attributes. Anything else throws
 * {@link IllegalArgumentException}, which the global handler turns into a 400.
 * <p>
 * Values are typed on the way through: {@code true}/{@code false} become booleans, numbers become
 * numbers, and ISO dates and date-times become UTC epoch seconds - which is how search documents
 * store instants, since the engine compares only numbers by range.
 */
public final class SearchParamsTranslator {

    private static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "q", "facets");
    private static final Set<String> OPERATIONS = Set.of(
            "eq", "noteq", "in", "notin", "gt", "gte", "lt", "lte", "between");
    private static final Set<String> SORT_DIRECTIONS = Set.of("asc", "desc");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final int MAX_ECHOED_KEY_LENGTH = 64;

    private SearchParamsTranslator() {
    }

    /**
     * Builds the filter for {@code searchParams}, or {@code null} when no filtering key is present.
     *
     * @throws IllegalArgumentException for a key that is not a filterable attribute, or a malformed value
     */
    public static SearchFilter toFilter(Map<String, String> searchParams, SearchIndexDefinition definition) {
        if (searchParams == null || searchParams.isEmpty()) {
            return null;
        }
        Map<String, String> attributes = normalisedLookup(definition.filterableAttributes());
        List<SearchFilter> filters = new ArrayList<>();
        searchParams.forEach((key, value) -> {
            if (key == null || value == null || value.isEmpty()
                    || RESERVED_PARAMS.contains(key.toLowerCase(Locale.ROOT))) {
                return;
            }
            ParsedKey parsed = parseKey(key);
            String attribute = attributes.get(normalise(parsed.field()));
            if (attribute == null) {
                throw new IllegalArgumentException("Unsupported search field: " + sanitise(parsed.field()));
            }
            filters.add(toFilter(attribute, parsed.operation(), value));
        });
        return SearchFilter.and(filters);
    }

    /**
     * Translates a {@code sort} expression - {@code field}, {@code field,dir}, or several such pairs
     * ({@code title,asc,created_at,desc}) - into sorts over sortable attributes.
     *
     * @throws IllegalArgumentException for a field that is not a sortable attribute
     */
    public static List<SearchSort> toSort(String sortExpression, SearchIndexDefinition definition) {
        if (sortExpression == null || sortExpression.isBlank()) {
            return List.of();
        }
        Map<String, String> attributes = normalisedLookup(definition.sortableAttributes());
        List<SearchSort> sorts = new ArrayList<>();
        String pendingField = null;
        for (String rawToken : sortExpression.split(",")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (SORT_DIRECTIONS.contains(lower)) {
                if (pendingField == null) {
                    throw new IllegalArgumentException("Sort direction without a field: " + sanitise(token));
                }
                sorts.add(new SearchSort(pendingField,
                        "desc".equals(lower) ? SearchSort.Direction.DESC : SearchSort.Direction.ASC));
                pendingField = null;
                continue;
            }
            if (pendingField != null) {
                sorts.add(SearchSort.asc(pendingField));
            }
            String attribute = attributes.get(normalise(token));
            if (attribute == null) {
                throw new IllegalArgumentException("Unsupported sort property: " + sanitise(token));
            }
            pendingField = attribute;
        }
        if (pendingField != null) {
            sorts.add(SearchSort.asc(pendingField));
        }
        return List.copyOf(sorts);
    }

    private static SearchFilter toFilter(String attribute, String operation, String rawValue) {
        return switch (operation) {
            case "noteq" -> SearchFilter.notEq(attribute, typed(rawValue));
            case "in" -> SearchFilter.in(attribute, typedList(rawValue));
            case "notin" -> SearchFilter.notIn(attribute, typedList(rawValue));
            case "gt" -> SearchFilter.gt(attribute, typed(rawValue));
            case "gte" -> SearchFilter.gte(attribute, typed(rawValue));
            case "lt" -> SearchFilter.lt(attribute, typed(rawValue));
            case "lte" -> SearchFilter.lte(attribute, typed(rawValue));
            case "between" -> {
                List<Object> bounds = typedList(rawValue);
                if (bounds.size() != 2) {
                    throw new IllegalArgumentException("A between filter takes exactly two values: from,to");
                }
                yield SearchFilter.between(attribute, bounds.get(0), bounds.get(1));
            }
            default -> SearchFilter.eq(attribute, typed(rawValue));
        };
    }

    private static List<Object> typedList(String rawValue) {
        List<Object> values = Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(SearchParamsTranslator::typed)
                .toList();
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one value");
        }
        return values;
    }

    static Object typed(String rawValue) {
        String value = rawValue.trim();
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        if (NUMBER.matcher(value).matches()) {
            return new BigDecimal(value);
        }
        Long epochSeconds = epochSeconds(value);
        if (epochSeconds != null) {
            return epochSeconds;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }

    private static Long epochSeconds(String value) {
        if (value.length() < 10 || !Character.isDigit(value.charAt(0))) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toEpochSecond();
        } catch (DateTimeParseException ignored) {
            // next format
        }
        try {
            return Instant.parse(value).getEpochSecond();
        } catch (DateTimeParseException ignored) {
            // next format
        }
        try {
            return LocalDateTime.parse(value).toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // next format
        }
        try {
            return LocalDate.parse(value).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private record ParsedKey(String field, String operation) {
    }

    private static ParsedKey parseKey(String key) {
        int lastUnderscore = key.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < key.length() - 1) {
            String operation = key.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT);
            if (OPERATIONS.contains(operation)) {
                return new ParsedKey(key.substring(0, lastUnderscore), operation);
            }
        }
        return new ParsedKey(key, "eq");
    }

    /** {@code organisation_uuid}, {@code organisationUuid} and {@code OrganisationUUID} all match. */
    private static String normalise(String field) {
        return field.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> normalisedLookup(List<String> attributes) {
        Map<String, String> lookup = new HashMap<>();
        for (String attribute : attributes) {
            lookup.put(normalise(attribute), attribute);
        }
        return lookup;
    }

    private static String sanitise(String key) {
        String cleaned = key.replaceAll("[^A-Za-z0-9_.]", "");
        return cleaned.length() > MAX_ECHOED_KEY_LENGTH ? cleaned.substring(0, MAX_ECHOED_KEY_LENGTH) : cleaned;
    }
}
