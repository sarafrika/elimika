package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** A step in a training application's history; stored upper case, serialised lower case. */
public enum TrainingApplicationEventType {
    SUBMITTED("submitted"),
    EDITED("edited"),
    OPENED_BY_CREATOR("opened_by_creator"),
    APPROVED("approved"),
    REJECTED("rejected"),
    REVOKED("revoked"),
    WITHDRAWN("withdrawn"),
    RATES_UPDATE_SUBMITTED("rates_update_submitted"),
    RATES_UPDATE_APPROVED("rates_update_approved"),
    RATES_UPDATE_REJECTED("rates_update_rejected"),
    RATES_UPDATE_WITHDRAWN("rates_update_withdrawn");

    private final String value;

    TrainingApplicationEventType(String value) {
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
    public static TrainingApplicationEventType fromValue(String value) {
        if (value != null) {
            for (TrainingApplicationEventType type : values()) {
                if (type.name().equals(value.trim().toUpperCase(Locale.ROOT))) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown TrainingApplicationEventType: " + value);
    }
}
