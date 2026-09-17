package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** How an applicant that lacks a training requirement would obtain it; stored upper case. */
public enum TrainingRequirementAcquisition {
    LEASE("lease"),
    HIRE("hire");

    private final String value;

    TrainingRequirementAcquisition(String value) {
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
    public static TrainingRequirementAcquisition fromValue(String value) {
        if (value != null) {
            for (TrainingRequirementAcquisition acquisition : values()) {
                if (acquisition.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
                    return acquisition;
                }
            }
        }
        throw new IllegalArgumentException("Unknown TrainingRequirementAcquisition: " + value);
    }
}
