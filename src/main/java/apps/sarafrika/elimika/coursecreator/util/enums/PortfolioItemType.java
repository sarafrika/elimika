package apps.sarafrika.elimika.coursecreator.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Kind of work shown in a course creator portfolio. */
public enum PortfolioItemType {
    PROJECT,
    PERFORMANCE,
    WORK_SAMPLE,
    MEDIA,
    OTHER;

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static PortfolioItemType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PortfolioItemType.valueOf(value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown PortfolioItemType: " + value);
        }
    }
}
