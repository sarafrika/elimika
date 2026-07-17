package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle of a course edit that is awaiting, or has been through, admin review.
 * <p>
 * Distinct from {@link ContentStatus}, which describes the course itself. A live course
 * with a PENDING edit stays PUBLISHED and admin-approved throughout review.
 */
public enum PendingEditStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn");

    private final String value;
    private static final Map<String, PendingEditStatus> VALUE_MAP = new HashMap<>();

    static {
        for (PendingEditStatus status : PendingEditStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    PendingEditStatus(String value) {
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
    public static PendingEditStatus fromValue(String value) {
        PendingEditStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown PendingEditStatus: " + value);
        }
        return status;
    }
}
