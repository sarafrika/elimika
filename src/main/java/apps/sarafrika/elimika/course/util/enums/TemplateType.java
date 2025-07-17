package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different types of certificate templates
 * Must match the database constraint: template_type VARCHAR(50) NOT NULL
 * Values: course_completion, program_completion, achievement
 */
public enum TemplateType {
    COURSE_COMPLETION("course_completion"),
    PROGRAM_COMPLETION("program_completion"),
    ACHIEVEMENT("achievement");

    private final String value;
    private static final Map<String, TemplateType> VALUE_MAP = new HashMap<>();

    static {
        for (TemplateType type : TemplateType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    TemplateType(String value) {
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
    public static TemplateType fromValue(String value) {
        TemplateType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown TemplateType: " + value);
        }
        return type;
    }

    public static TemplateType fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the template type
     */
    public String getDisplayName() {
        return switch (this) {
            case COURSE_COMPLETION -> "Course Completion Certificate";
            case PROGRAM_COMPLETION -> "Program Completion Certificate";
            case ACHIEVEMENT -> "Achievement Certificate";
        };
    }

    /**
     * Check if this template type is for completion certificates
     */
    public boolean isCompletionType() {
        return this == COURSE_COMPLETION || this == PROGRAM_COMPLETION;
    }

    /**
     * Get the database value (same as getValue())
     */
    public String getDatabaseValue() {
        return this.value;
    }

    /**
     * Create enum from database value (same as fromValue())
     */
    public static TemplateType fromDatabaseValue(String value) {
        return fromValue(value);
    }
}