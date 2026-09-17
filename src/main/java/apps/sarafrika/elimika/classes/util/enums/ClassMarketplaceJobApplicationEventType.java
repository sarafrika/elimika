package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** A step in a marketplace job application's history; stored upper case, serialised lower case. */
public enum ClassMarketplaceJobApplicationEventType {
    APPLIED("applied"),
    REAPPLIED("reapplied"),
    SHORTLISTED("shortlisted"),
    INTERVIEWING("interviewing"),
    OFFERED("offered"),
    HIRED("hired"),
    ASSIGNED("assigned"),
    REJECTED("rejected"),
    NOT_SELECTED("not_selected"),
    WITHDRAWN("withdrawn");

    private final String value;

    ClassMarketplaceJobApplicationEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /** The event a move into one of the organisation's review stages records. */
    public static ClassMarketplaceJobApplicationEventType forReviewStage(ClassMarketplaceJobApplicationStatus stage) {
        return switch (stage) {
            case SHORTLISTED -> SHORTLISTED;
            case INTERVIEWING -> INTERVIEWING;
            case OFFERED -> OFFERED;
            default -> throw new IllegalArgumentException("No history event defined for stage " + stage);
        };
    }

    @JsonCreator
    public static ClassMarketplaceJobApplicationEventType fromValue(String value) {
        if (value != null) {
            String normalised = value.trim().toUpperCase(Locale.ROOT);
            for (ClassMarketplaceJobApplicationEventType type : values()) {
                if (type.name().equals(normalised)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown ClassMarketplaceJobApplicationEventType: " + value);
    }
}
