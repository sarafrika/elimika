package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the various states of a student enrollment in a scheduled instance.
 * <p>
 * This enum follows the project's pattern for database-mapped enums with JSON serialization support.
 * It tracks the enrollment lifecycle from initial enrollment through attendance tracking to potential
 * cancellation.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public enum EnrollmentStatus {
    ENROLLED("ENROLLED", "Student is enrolled"),
    ATTENDED("ATTENDED", "Student attended the class"),
    ABSENT("ABSENT", "Student was absent"),
    CANCELLED("CANCELLED", "Enrollment was cancelled");
    
    private final String value;
    private final String description;
    private static final Map<String, EnrollmentStatus> VALUE_MAP = new HashMap<>();
    
    static {
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }
    
    EnrollmentStatus(String value, String description) {
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
    public static EnrollmentStatus fromValue(String value) {
        EnrollmentStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown EnrollmentStatus: " + value);
        }
        return status;
    }
}