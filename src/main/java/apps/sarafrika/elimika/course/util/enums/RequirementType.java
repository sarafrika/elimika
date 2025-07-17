package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different types of course requirements
 * Must match the database constraint: CHECK (requirement_type IN ('student', 'training_center', 'instructor'))
 */
public enum RequirementType {
    STUDENT("student"),
    TRAINING_CENTER("training_center"),
    INSTRUCTOR("instructor");

    private final String value;
    private static final Map<String, RequirementType> VALUE_MAP = new HashMap<>();

    static {
        for (RequirementType type : RequirementType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    RequirementType(String value) {
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
    public static RequirementType fromValue(String value) {
        RequirementType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown RequirementType: " + value);
        }
        return type;
    }

    public static RequirementType fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the requirement type
     */
    public String getDisplayName() {
        return switch (this) {
            case STUDENT -> "Student Requirement";
            case TRAINING_CENTER -> "Training Center Requirement";
            case INSTRUCTOR -> "Instructor Requirement";
        };
    }

    /**
     * Get the singular form for display purposes
     */
    public String getSingularDisplayName() {
        return switch (this) {
            case STUDENT -> "Student";
            case TRAINING_CENTER -> "Training Center";
            case INSTRUCTOR -> "Instructor";
        };
    }

    /**
     * Check if this requirement type applies to individuals
     */
    public boolean isIndividualRequirement() {
        return this == STUDENT || this == INSTRUCTOR;
    }

    /**
     * Check if this requirement type applies to organizations
     */
    public boolean isOrganizationalRequirement() {
        return this == TRAINING_CENTER;
    }

    /**
     * Check if this requirement type relates to course delivery
     */
    public boolean isDeliveryRelated() {
        return this == TRAINING_CENTER || this == INSTRUCTOR;
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
    public static RequirementType fromDatabaseValue(String value) {
        return fromValue(value);
    }
}