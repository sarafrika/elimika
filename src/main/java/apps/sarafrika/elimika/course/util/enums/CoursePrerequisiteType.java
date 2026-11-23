package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * How a learner qualifies for a course: no prerequisite, the instructor's permission, or the prior
 * courses listed in {@code course_prerequisites}.
 */
public enum CoursePrerequisiteType {
    NONE("NONE"),
    INSTRUCTOR_PERMISSION("INSTRUCTOR_PERMISSION"),
    COURSES("COURSES");

    private final String value;
    private static final Map<String, CoursePrerequisiteType> VALUE_MAP = new HashMap<>();

    static {
        for (CoursePrerequisiteType type : CoursePrerequisiteType.values()) {
            VALUE_MAP.put(type.value, type);
        }
    }

    CoursePrerequisiteType(String value) {
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
    public static CoursePrerequisiteType fromValue(String value) {
        if (value == null) {
            return null;
        }
        CoursePrerequisiteType type = VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown CoursePrerequisiteType: " + value);
        }
        return type;
    }

    public static CoursePrerequisiteType fromString(String value) {
        return fromValue(value);
    }
}
