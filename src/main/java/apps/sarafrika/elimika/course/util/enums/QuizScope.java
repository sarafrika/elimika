package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Indicates whether a quiz is the canonical course template or a class-specific clone.
 */
public enum QuizScope {
    COURSE_TEMPLATE("COURSE_TEMPLATE"),
    CLASS_CLONE("CLASS_CLONE");

    private final String value;
    private static final Map<String, QuizScope> VALUE_MAP = new HashMap<>();

    static {
        for (QuizScope scope : QuizScope.values()) {
            VALUE_MAP.put(scope.value, scope);
            VALUE_MAP.put(scope.value.toLowerCase(), scope);
        }
    }

    QuizScope(String value) {
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
    public static QuizScope fromValue(String value) {
        QuizScope scope = VALUE_MAP.get(value);
        if (scope == null) {
            throw new IllegalArgumentException("Unknown QuizScope: " + value);
        }
        return scope;
    }

    public static QuizScope fromString(String value) {
        return fromValue(value);
    }
}
