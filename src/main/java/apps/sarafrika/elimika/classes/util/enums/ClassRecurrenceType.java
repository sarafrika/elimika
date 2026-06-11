package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ClassRecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static ClassRecurrenceType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ClassRecurrenceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
