package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum ContentStatus {
    DRAFT("draft"),
    IN_REVIEW("in_review"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private final String value;
    private static final Map<String, ContentStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ContentStatus status : ContentStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    ContentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static ContentStatus fromValue(String value) {
        ContentStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ContentStatus: " + value);
        }
        return status;
    }

    public static ContentStatus fromString(String value) {
        return fromValue(value);
    }
}