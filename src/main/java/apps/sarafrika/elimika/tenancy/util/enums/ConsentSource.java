package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * How consent for an organisation affiliation was obtained.
 * <p>
 * Recorded on {@code user_organisation_domain_mapping} so that every affiliation can be
 * traced back to a consenting act.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
public enum ConsentSource {

    /** The user accepted an invitation themselves. */
    INVITATION("INVITATION", "Accepted an invitation"),

    /** A guardian consented on behalf of a minor. */
    GUARDIAN("GUARDIAN", "Guardian consented on behalf of a minor"),

    /** The user asked to join the organisation of their own accord. */
    SELF_JOIN("SELF_JOIN", "User joined of their own accord"),

    /** Created administratively for a staff domain. */
    ADMIN("ADMIN", "Created administratively"),

    /**
     * Pre-dates consent capture. These rows were created unilaterally by an organisation
     * and carry no record of the user agreeing.
     */
    LEGACY("LEGACY", "Pre-dates consent capture");

    private final String value;
    private final String description;

    ConsentSource(String value, String description) {
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
    public static ConsentSource fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ConsentSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ConsentSource: " + value);
        }
    }
}
