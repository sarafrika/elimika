package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle state for an instructor application against a marketplace class job.
 */
public enum ClassMarketplaceJobApplicationStatus {
    PENDING("pending"),
    SHORTLISTED("shortlisted"),
    INTERVIEWING("interviewing"),
    OFFERED("offered"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ASSIGNED("assigned"),
    NOT_SELECTED("not_selected"),
    WITHDRAWN("withdrawn");

    private final String value;
    private static final Map<String, ClassMarketplaceJobApplicationStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ClassMarketplaceJobApplicationStatus status : values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    ClassMarketplaceJobApplicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ClassMarketplaceJobApplicationStatus fromValue(String value) {
        ClassMarketplaceJobApplicationStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ClassMarketplaceJobApplicationStatus: " + value);
        }
        return status;
    }

    public boolean isFinal() {
        return this == REJECTED || this == ASSIGNED || this == NOT_SELECTED || this == WITHDRAWN;
    }

    /**
     * Whether the application is still live in the organisation's recruitment funnel.
     * A live application must not be resubmitted - doing so would reset the instructor's
     * hard-won position in the funnel back to PENDING.
     */
    public boolean isActive() {
        return !isFinal();
    }

    /**
     * Whether an instructor holding an application in this state may apply to the job again.
     * Rejected, passed-over and withdrawn applications are all reopenable; being assigned
     * is not, since the instructor already holds the job.
     */
    public boolean allowsReapplication() {
        return this == REJECTED || this == NOT_SELECTED || this == WITHDRAWN;
    }
}
