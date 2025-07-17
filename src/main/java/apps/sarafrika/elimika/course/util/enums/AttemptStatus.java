package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different statuses of quiz attempts
 * Must match the database constraint: CHECK (status IN ('in_progress', 'submitted', 'graded'))
 */
public enum AttemptStatus {
    IN_PROGRESS("in_progress"),
    SUBMITTED("submitted"),
    GRADED("graded");

    private final String value;
    private static final Map<String, AttemptStatus> VALUE_MAP = new HashMap<>();

    static {
        for (AttemptStatus status : AttemptStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    AttemptStatus(String value) {
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
    public static AttemptStatus fromValue(String value) {
        AttemptStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown AttemptStatus: " + value);
        }
        return status;
    }

    public static AttemptStatus fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the attempt status
     */
    public String getDisplayName() {
        return switch (this) {
            case IN_PROGRESS -> "In Progress";
            case SUBMITTED -> "Submitted";
            case GRADED -> "Graded";
        };
    }

    /**
     * Check if this status indicates the attempt is currently active
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if this status indicates the attempt has been submitted
     */
    public boolean isSubmitted() {
        return this == SUBMITTED || this == GRADED;
    }

    /**
     * Check if this status indicates the attempt has been graded
     */
    public boolean isGraded() {
        return this == GRADED;
    }

    /**
     * Check if this status indicates the attempt is completed (submitted or graded)
     */
    public boolean isCompleted() {
        return this == SUBMITTED || this == GRADED;
    }

    /**
     * Check if this status allows modifications to the attempt
     */
    public boolean allowsModifications() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if this status allows viewing of results
     */
    public boolean allowsResultViewing() {
        return this == GRADED;
    }

    /**
     * Check if this status is awaiting grading
     */
    public boolean isPendingGrading() {
        return this == SUBMITTED;
    }

    /**
     * Get the next logical status in the workflow
     */
    public AttemptStatus getNextStatus() {
        return switch (this) {
            case IN_PROGRESS -> SUBMITTED;
            case SUBMITTED -> GRADED;
            case GRADED -> GRADED; // Terminal state
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
    public static AttemptStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }
}