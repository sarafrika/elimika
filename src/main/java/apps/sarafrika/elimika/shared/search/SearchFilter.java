package apps.sarafrika.elimika.shared.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An engine-neutral filter over document attributes.
 * <p>
 * Filters are data, never strings: the engine adapter renders them into its own syntax and is the
 * only place that quotes and escapes values, so a value that arrived from a request can never change
 * the shape of the expression. Values are limited to {@link String}, {@link Number},
 * {@link Boolean} and {@link UUID}; anything else is rejected when the filter is built.
 * <p>
 * Build filters through the static factories ({@link #eq}, {@link #in}, {@link #and}, ...) rather
 * than the record constructors.
 */
public sealed interface SearchFilter {

    // ===== Factories =====

    static SearchFilter eq(String attribute, Object value) {
        return new Eq(attribute, value);
    }

    static SearchFilter notEq(String attribute, Object value) {
        return not(eq(attribute, value));
    }

    static SearchFilter in(String attribute, Collection<?> values) {
        return new In(attribute, values == null ? List.of() : new ArrayList<>(values));
    }

    static SearchFilter in(String attribute, Object... values) {
        return in(attribute, Arrays.asList(values));
    }

    static SearchFilter notIn(String attribute, Collection<?> values) {
        return not(in(attribute, values));
    }

    static SearchFilter gt(String attribute, Object value) {
        return new Range(attribute, null, null, value, null);
    }

    static SearchFilter gte(String attribute, Object value) {
        return new Range(attribute, value, null, null, null);
    }

    static SearchFilter lt(String attribute, Object value) {
        return new Range(attribute, null, null, null, value);
    }

    static SearchFilter lte(String attribute, Object value) {
        return new Range(attribute, null, value, null, null);
    }

    /** Inclusive on both ends. */
    static SearchFilter between(String attribute, Object from, Object to) {
        return new Range(attribute, from, to, null, null);
    }

    static SearchFilter isNull(String attribute) {
        return new IsNull(attribute);
    }

    /**
     * Documents whose {@code _geo} point lies within {@code meters} of the given point. The index must
     * list {@link SearchIndexDefinition#GEO_ATTRIBUTE} among its filterable attributes.
     */
    static SearchFilter geoRadius(double lat, double lng, int meters) {
        return new GeoRadius(lat, lng, meters);
    }

    static SearchFilter not(SearchFilter filter) {
        return new Not(filter);
    }

    /** ANDs the given filters, ignoring nulls. Returns {@code null} when nothing is left. */
    static SearchFilter and(SearchFilter... filters) {
        return and(Arrays.asList(filters));
    }

    /** ANDs the given filters, ignoring nulls. Returns {@code null} when nothing is left. */
    static SearchFilter and(Collection<SearchFilter> filters) {
        List<SearchFilter> parts = filters == null ? List.of()
                : filters.stream().filter(Objects::nonNull).toList();
        if (parts.isEmpty()) {
            return null;
        }
        return parts.size() == 1 ? parts.getFirst() : new And(parts);
    }

    /** ORs the given filters, ignoring nulls. Returns {@code null} when nothing is left. */
    static SearchFilter or(SearchFilter... filters) {
        return or(Arrays.asList(filters));
    }

    /** ORs the given filters, ignoring nulls. Returns {@code null} when nothing is left. */
    static SearchFilter or(Collection<SearchFilter> filters) {
        List<SearchFilter> parts = filters == null ? List.of()
                : filters.stream().filter(Objects::nonNull).toList();
        if (parts.isEmpty()) {
            return null;
        }
        return parts.size() == 1 ? parts.getFirst() : new Or(parts);
    }

    // ===== Node types =====

    record Eq(String attribute, Object value) implements SearchFilter {
        public Eq {
            Objects.requireNonNull(attribute, "attribute");
            value = requireSupportedValue(value);
        }
    }

    record In(String attribute, List<Object> values) implements SearchFilter {
        public In {
            Objects.requireNonNull(attribute, "attribute");
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("An IN filter on " + attribute + " needs at least one value");
            }
            values = values.stream().map(SearchFilter::requireSupportedValue).toList();
        }
    }

    /** At least one bound is required; unset bounds are {@code null}. */
    record Range(String attribute, Object gte, Object lte, Object gt, Object lt) implements SearchFilter {
        public Range {
            Objects.requireNonNull(attribute, "attribute");
            if (gte == null && lte == null && gt == null && lt == null) {
                throw new IllegalArgumentException("A range filter on " + attribute + " needs at least one bound");
            }
            gte = gte == null ? null : requireSupportedValue(gte);
            lte = lte == null ? null : requireSupportedValue(lte);
            gt = gt == null ? null : requireSupportedValue(gt);
            lt = lt == null ? null : requireSupportedValue(lt);
        }
    }

    record IsNull(String attribute) implements SearchFilter {
        public IsNull {
            Objects.requireNonNull(attribute, "attribute");
        }
    }

    /**
     * A circle around a point on the index's {@code _geo} attribute. Coordinates are WGS84 degrees;
     * the radius is in metres and must be positive.
     */
    record GeoRadius(double lat, double lng, int meters) implements SearchFilter {
        public GeoRadius {
            SearchGeoPoint.requireValid(lat, lng);
            if (meters <= 0) {
                throw new IllegalArgumentException("A geo radius must be a positive number of metres");
            }
        }
    }

    record Not(SearchFilter filter) implements SearchFilter {
        public Not {
            Objects.requireNonNull(filter, "filter");
        }
    }

    record And(List<SearchFilter> filters) implements SearchFilter {
        public And {
            filters = List.copyOf(filters);
            if (filters.isEmpty()) {
                throw new IllegalArgumentException("An AND filter needs at least one operand");
            }
        }
    }

    record Or(List<SearchFilter> filters) implements SearchFilter {
        public Or {
            filters = List.copyOf(filters);
            if (filters.isEmpty()) {
                throw new IllegalArgumentException("An OR filter needs at least one operand");
            }
        }
    }

    private static Object requireSupportedValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Filter values cannot be null; use isNull(attribute)");
        }
        if (value instanceof String || value instanceof Boolean || value instanceof UUID) {
            return value;
        }
        if (value instanceof Number number) {
            if (number instanceof Double d && (d.isNaN() || d.isInfinite())
                    || number instanceof Float f && (f.isNaN() || f.isInfinite())) {
                throw new IllegalArgumentException("Filter values must be finite numbers");
            }
            return value;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        throw new IllegalArgumentException("Unsupported filter value type: " + value.getClass().getSimpleName());
    }
}
