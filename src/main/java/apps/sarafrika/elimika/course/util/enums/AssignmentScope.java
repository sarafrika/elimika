package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the scope at which an assignment is applicable.
 * COURSE_TEMPLATE assignments are managed at the course level and act as blueprints.
 * CLASS_CLONE assignments are derived copies owned by a specific class.
 */
public enum AssignmentScope {
    COURSE_TEMPLATE("COURSE_TEMPLATE"),
    CLASS_CLONE("CLASS_CLONE");

    private final String value;
    private static final Map<String, AssignmentScope> VALUE_MAP = new HashMap<>();

    static {
        for (AssignmentScope scope : AssignmentScope.values()) {
            VALUE_MAP.put(scope.value, scope);
            VALUE_MAP.put(scope.value.toLowerCase(), scope);
        }
    }

    AssignmentScope(String value) {
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
    public static AssignmentScope fromValue(String value) {
        AssignmentScope scope = VALUE_MAP.get(value);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown AssignmentScope: " + value);
        }
        return scope;
    }

    public static AssignmentScope fromString(String value) {
        return fromValue(value);
    }
}
