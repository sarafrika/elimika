package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the resource types that course creators can mandate for training delivery.
 */
public enum CourseTrainingRequirementType {
    MATERIAL("material"),
    EQUIPMENT("equipment"),
    FACILITY("facility"),
    OTHER("other");

    private final String value;
    private static final Map<String, CourseTrainingRequirementType> VALUE_MAP = new HashMap<>();

    static {
        for (CourseTrainingRequirementType type : CourseTrainingRequirementType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    CourseTrainingRequirementType(String value) {
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
    public static CourseTrainingRequirementType fromValue(String value) {
        CourseTrainingRequirementType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown CourseTrainingRequirementType: " + value);
        }
        return type;
    }

    public String getDisplayName() {
        return switch (this) {
            case MATERIAL -> "Material";
            case EQUIPMENT -> "Equipment";
            case FACILITY -> "Facility";
            case OTHER -> "Other";
        };
    }
}
