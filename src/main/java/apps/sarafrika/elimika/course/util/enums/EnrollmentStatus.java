package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different enrollment statuses for course enrollments
 * Must match the database constraint: CHECK (status IN ('active', 'completed', 'dropped', 'suspended'))
 */
public enum EnrollmentStatus {
    ACTIVE("active"),
    COMPLETED("completed"),
    DROPPED("dropped"),
    SUSPENDED("suspended");

    private final String value;
    private static final Map<String, EnrollmentStatus> VALUE_MAP = new HashMap<>();

    static {
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    EnrollmentStatus(String value) {
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
    public static EnrollmentStatus fromValue(String value) {
        EnrollmentStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown EnrollmentStatus: " + value);
        }
        return status;
    }

    public static EnrollmentStatus fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the enrollment status
     */
    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case DROPPED -> "Dropped";
            case SUSPENDED -> "Suspended";
        };
    }

    /**
     * Check if this status indicates the enrollment is currently active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Check if this status indicates the enrollment has been completed successfully
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * Check if this status indicates the enrollment has been terminated
     */
    public boolean isTerminated() {
        return this == DROPPED || this == SUSPENDED;
    }

    /**
     * Check if this status allows the student to access course content
     */
    public boolean allowsAccess() {
        return this == ACTIVE || this == COMPLETED;
    }

    /**
     * Check if this status allows enrollment progress updates
     */
    public boolean allowsProgressUpdates() {
        return this == ACTIVE;
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
    public static EnrollmentStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }
}