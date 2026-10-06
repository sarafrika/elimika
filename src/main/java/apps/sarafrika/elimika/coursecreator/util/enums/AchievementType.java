package apps.sarafrika.elimika.coursecreator.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Kind of achievement in a course creator skills wallet. */
public enum AchievementType {
    AWARD,
    MILESTONE,
    COMPETITION,
    UNLOCKED_SKILL,
    RECOGNITION;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static AchievementType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AchievementType.valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AchievementType: " + value);
        }
    }
}
