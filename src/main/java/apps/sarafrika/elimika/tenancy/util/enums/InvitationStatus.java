package apps.sarafrika.elimika.tenancy.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Lifecycle states of an organisation invitation.
 * <p>
 * An invitation is an offer, not an affiliation. Only {@link #ACCEPTED} results in a
 * {@code user_organisation_domain_mapping} row being written.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
public enum InvitationStatus {

    /** Sent and awaiting a decision from the invitee. */
    PENDING("PENDING", "Awaiting a decision from the invitee"),

    /**
     * The invitee declared a date of birth below the configured age gate, so the offer now
     * awaits consent from their guardian. No affiliation exists in this state.
     */
    AWAITING_GUARDIAN_CONSENT("AWAITING_GUARDIAN_CONSENT", "Awaiting guardian consent for a minor"),

    /** Consent given; the affiliation has been created. */
    ACCEPTED("ACCEPTED", "Accepted and affiliation created"),

    /** Explicitly turned down by the invitee or their guardian. */
    DECLINED("DECLINED", "Declined by the invitee or guardian"),

    /** Lapsed without a decision. */
    EXPIRED("EXPIRED", "Expired without a decision"),

    /** Withdrawn by the inviting organisation. */
    REVOKED("REVOKED", "Withdrawn by the organisation");

    private final String value;
    private final String description;

    InvitationStatus(String value, String description) {
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

    /**
     * States in which an invitation can still be acted upon. Used to enforce one live
     * offer per email per organisation.
     */
    public boolean isLive() {
        return this == PENDING || this == AWAITING_GUARDIAN_CONSENT;
    }

    @JsonCreator
    public static InvitationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return InvitationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown InvitationStatus: " + value);
        }
    }
}
