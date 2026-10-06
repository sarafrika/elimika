package apps.sarafrika.elimika.shared.utils.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Whether a user may act in a domain they hold. A domain mapping grants access only once it is
 * {@link #APPROVED}; every other state keeps the user on the pending-approval screen.
 */
public enum DomainApprovalStatus {

    /** Requested at registration or by creating a profile, awaiting a platform admin. */
    PENDING("PENDING"),

    /** Approved by a platform admin, or granted by a flow that needs no review. */
    APPROVED("APPROVED"),

    /** Turned down by a platform admin before it was ever approved. */
    REJECTED("REJECTED"),

    /** Approved once, then withdrawn by a platform admin. */
    SUSPENDED("SUSPENDED");

    private final String value;

    DomainApprovalStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DomainApprovalStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return DomainApprovalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown DomainApprovalStatus: " + value);
        }
    }
}
