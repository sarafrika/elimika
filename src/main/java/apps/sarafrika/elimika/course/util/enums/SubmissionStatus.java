package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the various states of an assignment submission
 * Must match the database constraint: CHECK (status IN ('draft', 'submitted', 'graded', 'returned'))
 */
public enum SubmissionStatus {
    DRAFT("draft"),
    SUBMITTED("submitted"),
    GRADED("graded"),
    RETURNED("returned");

    private final String value;
    private static final Map<String, SubmissionStatus> VALUE_MAP = new HashMap<>();

    static {
        for (SubmissionStatus status : SubmissionStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    SubmissionStatus(String value) {
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

    @JsonCreator
    public static SubmissionStatus fromValue(String value) {
        SubmissionStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown SubmissionStatus: " + value);
        }
        return status;
    }

    public static SubmissionStatus fromString(String value) {
        return fromValue(value);
    }

    /**
     * Check if this status indicates the submission is pending grading
     */
    public boolean isPendingGrading() {
        return this == SUBMITTED;
    }

    /**
     * Check if this status indicates the submission is completed
     */
    public boolean isCompleted() {
        return this == GRADED;
    }

    /**
     * Check if this status allows for revision
     */
    public boolean allowsRevision() {
        return this == DRAFT || this == RETURNED;
    }

    /**
     * Get user-friendly display name for the status
     */
    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Draft";
            case SUBMITTED -> "Submitted";
            case GRADED -> "Graded";
            case RETURNED -> "Returned for Revision";
        };
    }

    /**
     * Get the database value (same as getValue())
     */
    public String getDatabaseValue() {
        return this.value;
    }

    /**
     * Create enum from database value (same as fromValue())
     */
    public static SubmissionStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }
}