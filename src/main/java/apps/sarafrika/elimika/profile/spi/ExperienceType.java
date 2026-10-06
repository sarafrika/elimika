package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Kind of experience in a course creator skills wallet. */
public enum ExperienceType {
    TRAINING,
    WORK,
    VOLUNTEERING,
    PROJECT;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static ExperienceType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ExperienceType.valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ExperienceType: " + value);
        }
    }
}
