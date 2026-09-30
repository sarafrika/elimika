package apps.sarafrika.elimika.shared.search;

import java.util.Objects;

/**
 * One ordering applied to a search. The field must be one of the index's sortable attributes.
 * <p>
 * A distance sort ({@link #geoPoint}) orders by distance from {@code origin} over the index's
 * {@link SearchIndexDefinition#GEO_ATTRIBUTE} attribute, which must then be sortable; its hits carry
 * {@link SearchHit#geoDistanceMeters()}.
 *
 * @param field     the attribute to order by; {@link SearchIndexDefinition#GEO_ATTRIBUTE} for a distance sort
 * @param direction ascending when {@code null}
 * @param origin    the point distances are measured from; set exactly when {@code field} is {@code _geo}
 */
public record SearchSort(String field, Direction direction, SearchGeoPoint origin) {

    public enum Direction {
        ASC,
        DESC
    }

    public SearchSort {
        Objects.requireNonNull(field, "field");
        direction = direction == null ? Direction.ASC : direction;
        boolean geo = SearchIndexDefinition.GEO_ATTRIBUTE.equals(field);
        if (geo && origin == null) {
            throw new IllegalArgumentException("A sort on _geo needs an origin; use SearchSort.geoPoint(lat, lng)");
        }
        if (!geo && origin != null) {
            throw new IllegalArgumentException("Only a sort on _geo takes an origin");
        }
    }

    public SearchSort(String field, Direction direction) {
        this(field, direction, null);
    }

    public static SearchSort asc(String field) {
        return new SearchSort(field, Direction.ASC);
    }

    public static SearchSort desc(String field) {
        return new SearchSort(field, Direction.DESC);
    }

    /** Nearest first: ascending distance from the given point. */
    public static SearchSort geoPoint(double lat, double lng) {
        return new SearchSort(SearchIndexDefinition.GEO_ATTRIBUTE, Direction.ASC, new SearchGeoPoint(lat, lng));
    }

    public boolean isGeo() {
        return origin != null;
    }
}
