package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** What kind of body an organisation is; chosen during organisation onboarding. */
public enum OrganisationType {

    SCHOOL_OR_TRAINING_INSTITUTION("SCHOOL_OR_TRAINING_INSTITUTION", "School / Training institution"),
    COLLEGE_OR_UNIVERSITY("COLLEGE_OR_UNIVERSITY", "College / University"),
    COMPANY_OR_EMPLOYER("COMPANY_OR_EMPLOYER", "Company / Employer"),
    FOUNDATION_OR_NGO("FOUNDATION_OR_NGO", "Foundation / NGO"),
    GOVERNMENT_AGENCY("GOVERNMENT_AGENCY", "Government agency");

    private final String value;
    private final String description;

    OrganisationType(String value, String description) {
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
    public static OrganisationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OrganisationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown OrganisationType: " + value);
        }
    }
}
