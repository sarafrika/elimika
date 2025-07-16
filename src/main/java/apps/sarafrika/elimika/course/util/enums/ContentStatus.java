package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentStatus {
    DRAFT("draft"),
    IN_REVIEW("in_review"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String value;

    ContentStatus(String value) {
        this.value = value;
    }

    @JsonValue  // For JSON serialization
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static ContentStatus fromValue(String value) {
        for (ContentStatus status : ContentStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ContentStatus: " + value);
    }

    public static ContentStatus fromString(String value) {
        return fromValue(value);
    }
}