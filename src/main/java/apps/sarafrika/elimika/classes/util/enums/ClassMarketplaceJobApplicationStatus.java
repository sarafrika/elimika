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
    HIRED("hired"),
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
     * Whether this state closes an application without carrying it any further forward.
     * Refusing a candidate is never a shortcut through the funnel, so an exit is open from
     * every live stage rather than from one particular predecessor.
     */
    public boolean isExit() {
        return this == REJECTED || this == NOT_SELECTED || this == WITHDRAWN;
    }

    /**
     * The one stage an application must already hold before it may enter this one:
     * applied - shortlisted - interviewing - offered - hired, then assigned once the class
     * is created. PENDING is where the funnel starts and the exits belong to no single stage.
     */
    public ClassMarketplaceJobApplicationStatus previousStage() {
        return switch (this) {
            case SHORTLISTED -> PENDING;
            case INTERVIEWING -> SHORTLISTED;
            case OFFERED -> INTERVIEWING;
            case HIRED -> OFFERED;
            case ASSIGNED -> HIRED;
            case PENDING, REJECTED, NOT_SELECTED, WITHDRAWN -> null;
        };
    }

    /**
     * Whether an application sitting at {@code current} may be moved into this state. Every
     * candidate is looked at at each stage, so the funnel is walked one step at a time and a
     * closed application is not walked at all.
     */
    public boolean isReachableFrom(ClassMarketplaceJobApplicationStatus current) {
        if (current == null || !current.isActive()) {
            return false;
        }
        return isExit() || current == previousStage();
    }

    /**
     * Whether the organisation has committed to this instructor. The commitment is what
     * attaches them to the organisation, so it outlives the job's recruitment window.
     */
    public boolean isHire() {
        return this == HIRED || this == ASSIGNED;
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
