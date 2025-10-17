package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes which party is responsible for providing a training requirement.
 */
public enum CourseTrainingRequirementProvider {
    COURSE_CREATOR("course_creator"),
    INSTRUCTOR("instructor"),
    ORGANISATION("organisation"),
    STUDENT("student");

    private final String value;
    private static final Map<String, CourseTrainingRequirementProvider> VALUE_MAP = new HashMap<>();

    static {
        for (CourseTrainingRequirementProvider provider : values()) {
            VALUE_MAP.put(provider.value, provider);
            VALUE_MAP.put(provider.value.toUpperCase(), provider);
        }
    }

    CourseTrainingRequirementProvider(String value) {
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
    public static CourseTrainingRequirementProvider fromValue(String value) {
        CourseTrainingRequirementProvider provider = VALUE_MAP.get(value);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown CourseTrainingRequirementProvider: " + value);
        }
        return provider;
    }
}
