package apps.sarafrika.elimika.shared.search;

import java.util.Objects;

/**
 * One ordering applied to a search. The field must be one of the index's sortable attributes.
 */
public record SearchSort(String field, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }

    public SearchSort {
        Objects.requireNonNull(field, "field");
        direction = direction == null ? Direction.ASC : direction;
    }

    public static SearchSort asc(String field) {
        return new SearchSort(field, Direction.ASC);
    }

    public static SearchSort desc(String field) {
        return new SearchSort(field, Direction.DESC);
    }
}
