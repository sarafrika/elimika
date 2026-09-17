package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Lifecycle of a proposed rate update; stored upper case, serialised lower case. */
public enum TrainingRateUpdateStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    WITHDRAWN("withdrawn");

    private final String value;

    TrainingRateUpdateStatus(String value) {
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
    public static TrainingRateUpdateStatus fromValue(String value) {
        if (value != null) {
            for (TrainingRateUpdateStatus status : values()) {
                if (status.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unknown TrainingRateUpdateStatus: " + value);
    }
}
