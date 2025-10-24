package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the lifecycle status of a course training application.
 */
public enum CourseTrainingApplicationStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    REVOKED("revoked");

    private final String value;
    private static final Map<String, CourseTrainingApplicationStatus> VALUE_MAP = new HashMap<>();

    static {
        for (CourseTrainingApplicationStatus status : CourseTrainingApplicationStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    CourseTrainingApplicationStatus(String value) {
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
    public static CourseTrainingApplicationStatus fromValue(String value) {
        CourseTrainingApplicationStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown CourseTrainingApplicationStatus: " + value);
        }
        return status;
    }

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == REVOKED;
    }
}
