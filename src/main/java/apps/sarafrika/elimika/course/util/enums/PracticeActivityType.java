package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum PracticeActivityType {
    EXERCISE("EXERCISE"),
    DISCUSSION("DISCUSSION"),
    CASE_STUDY("CASE_STUDY"),
    ROLE_PLAY("ROLE_PLAY"),
    REFLECTION("REFLECTION"),
    HANDS_ON("HANDS_ON");

    private final String value;
    private static final Map<String, PracticeActivityType> VALUE_MAP = new HashMap<>();

    static {
        for (PracticeActivityType type : PracticeActivityType.values()) {
            VALUE_MAP.put(type.value, type);
        }
    }

    PracticeActivityType(String value) {
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
    public static PracticeActivityType fromValue(String value) {
        if (value == null) {
            return null;
        }
        PracticeActivityType type = VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown PracticeActivityType: " + value);
        }
        return type;
    }

    public static PracticeActivityType fromString(String value) {
        return fromValue(value);
    }
}
