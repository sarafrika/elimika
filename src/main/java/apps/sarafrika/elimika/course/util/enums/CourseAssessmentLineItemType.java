package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CourseAssessmentLineItemType {
    ASSIGNMENT("assignment"),
    QUIZ("quiz"),
    ATTENDANCE("attendance"),
    PROJECT("project"),
    DISCUSSION("discussion"),
    EXAM("exam"),
    PRACTICAL("practical"),
    PERFORMANCE("performance"),
    PARTICIPATION("participation"),
    MANUAL("manual");

    private final String value;

    CourseAssessmentLineItemType(String value) {
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
    public static CourseAssessmentLineItemType fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CourseAssessmentLineItemType itemType : values()) {
            if (itemType.value.equals(normalized)) {
                return itemType;
            }
        }

        throw new IllegalArgumentException("Unknown CourseAssessmentLineItemType: " + value);
    }

    public static CourseAssessmentLineItemType fromString(String value) {
        return fromValue(value);
    }
}
