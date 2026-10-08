package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Who an age group belongs to: an instructor's or organisation's saved set, or the copy inside a
 * training application (which alone carries per-lesson hours).
 */
public enum AgeGroupOwnerType {
    INSTRUCTOR("instructor"),
    ORGANISATION("organisation"),
    COURSE_TRAINING_APPLICATION("course_training_application"),
    PROGRAM_TRAINING_APPLICATION("program_training_application");

    private final String value;

    AgeGroupOwnerType(String value) {
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

    public boolean isSaved() {
        return this == INSTRUCTOR || this == ORGANISATION;
    }

    public static AgeGroupOwnerType forApplication(TrainingApplicationType type) {
        return type == TrainingApplicationType.PROGRAM ? PROGRAM_TRAINING_APPLICATION : COURSE_TRAINING_APPLICATION;
    }

    @JsonCreator
    public static AgeGroupOwnerType fromValue(String value) {
        if (value != null) {
            for (AgeGroupOwnerType type : values()) {
                if (type.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown AgeGroupOwnerType: " + value);
    }
}
