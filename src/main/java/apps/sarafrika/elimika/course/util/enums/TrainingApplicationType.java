package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Whether a training application targets a course or a training program. */
public enum TrainingApplicationType {
    COURSE("course"),
    PROGRAM("program");

    private final String value;

    TrainingApplicationType(String value) {
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
    public static TrainingApplicationType fromValue(String value) {
        if (value != null) {
            for (TrainingApplicationType type : values()) {
                if (type.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown TrainingApplicationType: " + value);
    }
}
