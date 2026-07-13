package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum ModerationAction {
    APPROVED("approved"),
    REJECTED("rejected"),
    REVOKED("revoked");

    private final String value;
    private static final Map<String, ModerationAction> VALUE_MAP = new HashMap<>();

    static {
        for (ModerationAction action : ModerationAction.values()) {
            VALUE_MAP.put(action.value, action);
            VALUE_MAP.put(action.value.toUpperCase(), action);
        }
    }

    ModerationAction(String value) {
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
    public static ModerationAction fromValue(String value) {
        ModerationAction action = VALUE_MAP.get(value);
        if (action == null) {
            throw new IllegalArgumentException("Unknown ModerationAction: " + value);
        }
        return action;
    }
}
