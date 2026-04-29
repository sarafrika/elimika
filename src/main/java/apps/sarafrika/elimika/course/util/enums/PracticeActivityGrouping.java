package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum PracticeActivityGrouping {
    INDIVIDUAL("INDIVIDUAL"),
    PAIR("PAIR"),
    SMALL_GROUP("SMALL_GROUP"),
    WHOLE_CLASS("WHOLE_CLASS");

    private final String value;
    private static final Map<String, PracticeActivityGrouping> VALUE_MAP = new HashMap<>();

    static {
        for (PracticeActivityGrouping grouping : PracticeActivityGrouping.values()) {
            VALUE_MAP.put(grouping.value, grouping);
        }
    }

    PracticeActivityGrouping(String value) {
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
    public static PracticeActivityGrouping fromValue(String value) {
        if (value == null) {
            return null;
        }
        PracticeActivityGrouping grouping = VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (grouping == null) {
            throw new IllegalArgumentException("Unknown PracticeActivityGrouping: " + value);
        }
        return grouping;
    }

    public static PracticeActivityGrouping fromString(String value) {
        return fromValue(value);
    }
}
