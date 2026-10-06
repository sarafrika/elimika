package apps.sarafrika.elimika.coursecreator.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CourseCreatorVerificationStatus {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    REVOKED("REVOKED");

    private final String value;

    CourseCreatorVerificationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean awaitingReview() {
        return this == SUBMITTED;
    }

    @JsonCreator
    public static CourseCreatorVerificationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return CourseCreatorVerificationStatus.valueOf(normalize(value));
    }

    private static String normalize(String value) {
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
