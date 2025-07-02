package apps.sarafrika.elimika.instructor.util.enums;

import lombok.Getter;

/**
 * Enum for skill proficiency levels
 * Must match the database enum: proficiency_level_enum
 */
@Getter
public enum ProficiencyLevel {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert");

    private final String displayName;

    ProficiencyLevel(String displayName) {
        this.displayName = displayName;
    }
    /**
     * Get enum from string value (case insensitive)
     */
    public static ProficiencyLevel fromString(String value) {
        if (value == null) {
            return null;
        }

        for (ProficiencyLevel level : ProficiencyLevel.values()) {
            if (level.name().equalsIgnoreCase(value) ||
                    level.displayName.equalsIgnoreCase(value)) {
                return level;
            }
        }

        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}