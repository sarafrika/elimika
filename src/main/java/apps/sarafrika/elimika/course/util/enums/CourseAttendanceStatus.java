package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CourseAttendanceStatus {
    ATTENDED("attended"),
    ABSENT("absent");

    private final String value;

    CourseAttendanceStatus(String value) {
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
    public static CourseAttendanceStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CourseAttendanceStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown CourseAttendanceStatus: " + value);
    }
}
