package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** What an organisation intends to do on the platform; chosen during organisation onboarding. */
public enum OrganisationCapability {

    TRAIN("TRAIN", "Offer training: courses, schedules and students"),
    PUBLISH("PUBLISH", "Publish courses: create and license content"),
    FUND("FUND", "Sponsor learning: fund students and programmes"),
    HIRE("HIRE", "Hire & recruit: recruit instructors and talent");

    private final String value;
    private final String description;

    OrganisationCapability(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static OrganisationCapability fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OrganisationCapability.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown OrganisationCapability: " + value);
        }
    }
}
