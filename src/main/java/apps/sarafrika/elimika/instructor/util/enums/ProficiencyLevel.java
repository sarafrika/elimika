package apps.sarafrika.elimika.instructor.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum for skill proficiency levels
 * Must match the database enum: proficiency_level_enum
 */
@Getter
public enum ProficiencyLevel {
    BEGINNER("beginner", "Beginner"),
    INTERMEDIATE("intermediate", "Intermediate"),
    ADVANCED("advanced", "Advanced"),
    EXPERT("expert", "Expert");

    private final String value;
    private final String displayName;
    private static final Map<String, ProficiencyLevel> VALUE_MAP = new HashMap<>();

    static {
        for (ProficiencyLevel level : ProficiencyLevel.values()) {
            VALUE_MAP.put(level.value, level);
            VALUE_MAP.put(level.value.toUpperCase(), level);
            VALUE_MAP.put(level.name(), level);
        }
    }

    ProficiencyLevel(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProficiencyLevel fromValue(String value) {
        ProficiencyLevel level = VALUE_MAP.get(value);
        if (level == null) {
            throw new IllegalArgumentException("Unknown ProficiencyLevel: " + value);
        }
        return level;
    }

    public static ProficiencyLevel fromString(String value) {
        return fromValue(value);
    }
}