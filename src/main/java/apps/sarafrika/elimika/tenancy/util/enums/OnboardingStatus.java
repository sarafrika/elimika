package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Where a user's onboarding for one domain stands; derived, never stored. */
public enum OnboardingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,
    APPROVED,
    REJECTED,
    SUSPENDED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static OnboardingStatus fromValue(String value) {
        return value == null ? null : OnboardingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
