package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum ModerationContentType {
    COURSE("course"),
    TRAINING_PROGRAM("training_program");

    private final String value;
    private static final Map<String, ModerationContentType> VALUE_MAP = new HashMap<>();

    static {
        for (ModerationContentType type : ModerationContentType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    ModerationContentType(String value) {
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
    public static ModerationContentType fromValue(String value) {
        ModerationContentType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown ModerationContentType: " + value);
        }
        return type;
    }
}
