package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * How a class-level assessment schedule derives its timing from the base template.
 */
public enum ClassAssessmentReleaseStrategy {
    INHERITED("INHERITED"),
    CUSTOM("CUSTOM"),
    CLONE("CLONE");

    private final String value;
    private static final Map<String, ClassAssessmentReleaseStrategy> VALUE_MAP = new HashMap<>();

    static {
        for (ClassAssessmentReleaseStrategy strategy : ClassAssessmentReleaseStrategy.values()) {
            VALUE_MAP.put(strategy.value, strategy);
            VALUE_MAP.put(strategy.value.toLowerCase(), strategy);
        }
    }

    ClassAssessmentReleaseStrategy(String value) {
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
    public static ClassAssessmentReleaseStrategy fromValue(String value) {
        ClassAssessmentReleaseStrategy strategy = VALUE_MAP.get(value);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown ClassAssessmentReleaseStrategy: " + value);
        }
        return strategy;
    }

    public static ClassAssessmentReleaseStrategy fromString(String value) {
        return fromValue(value);
    }
}
