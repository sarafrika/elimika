package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** A learner's pass/fail result on a course or program; decided once every required item is graded. */
public enum CourseResultStatus {
    IN_PROGRESS,
    PASSED,
    FAILED;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static CourseResultStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CourseResultStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CourseResultStatus: " + value);
        }
    }
}
