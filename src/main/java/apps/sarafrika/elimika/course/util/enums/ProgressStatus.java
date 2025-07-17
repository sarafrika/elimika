package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different progress statuses for lesson progress tracking
 * Must match the database constraint: CHECK (status IN ('not_started', 'in_progress', 'completed', 'skipped'))
 */
public enum ProgressStatus {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    SKIPPED("skipped");

    private final String value;
    private static final Map<String, ProgressStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ProgressStatus status : ProgressStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    ProgressStatus(String value) {
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
    public static ProgressStatus fromValue(String value) {
        ProgressStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ProgressStatus: " + value);
        }
        return status;
    }

    public static ProgressStatus fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the progress status
     */
    public String getDisplayName() {
        return switch (this) {
            case NOT_STARTED -> "Not Started";
            case IN_PROGRESS -> "In Progress";
            case COMPLETED -> "Completed";
            case SKIPPED -> "Skipped";
        };
    }

    /**
     * Check if this status indicates the lesson has been started
     */
    public boolean isStarted() {
        return this != NOT_STARTED;
    }

    /**
     * Check if this status indicates the lesson is currently active
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if this status indicates the lesson has been finished (completed or skipped)
     */
    public boolean isFinished() {
        return this == COMPLETED || this == SKIPPED;
    }

    /**
     * Check if this status indicates the lesson was completed successfully
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * Check if this status allows progression to next lesson
     */
    public boolean allowsProgression() {
        return this == COMPLETED || this == SKIPPED;
    }

    /**
     * Check if this status contributes to completion percentage
     */
    public boolean contributesToCompletion() {
        return this == COMPLETED;
    }

    /**
     * Get the completion percentage for this status (0-100)
     */
    public int getCompletionPercentage() {
        return switch (this) {
            case NOT_STARTED -> 0;
            case IN_PROGRESS -> 50;
            case COMPLETED -> 100;
            case SKIPPED -> 0; // Skipped doesn't count as completion
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
    public static ProgressStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }
}