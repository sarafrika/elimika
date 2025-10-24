package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the applicant types that can request permission to deliver a course.
 */
public enum CourseTrainingApplicantType {
    INSTRUCTOR("instructor"),
    ORGANISATION("organisation");

    private final String value;
    private static final Map<String, CourseTrainingApplicantType> VALUE_MAP = new HashMap<>();

    static {
        for (CourseTrainingApplicantType type : CourseTrainingApplicantType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    CourseTrainingApplicantType(String value) {
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
    public static CourseTrainingApplicantType fromValue(String value) {
        CourseTrainingApplicantType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown CourseTrainingApplicantType: " + value);
        }
        return type;
    }

    public String getDisplayName() {
        return switch (this) {
            case INSTRUCTOR -> "Instructor";
            case ORGANISATION -> "Organisation";
        };
    }
}
