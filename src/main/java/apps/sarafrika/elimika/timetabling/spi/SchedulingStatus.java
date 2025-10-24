package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the various states of a scheduled instance in the timetabling system.
 * <p>
 * This enum follows the project's pattern for database-mapped enums with JSON serialization support.
 * It provides a comprehensive set of states that a scheduled class instance can be in throughout
 * its lifecycle from creation to completion or cancellation.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public enum SchedulingStatus {
    SCHEDULED("SCHEDULED", "Class is scheduled"),
    ONGOING("ONGOING", "Class is currently in progress"),
    COMPLETED("COMPLETED", "Class has been completed"),
    CANCELLED("CANCELLED", "Class has been cancelled");
    
    private final String value;
    private final String description;
    private static final Map<String, SchedulingStatus> VALUE_MAP = new HashMap<>();
    
    static {
        for (SchedulingStatus status : SchedulingStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }
    
    SchedulingStatus(String value, String description) {
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
    public static SchedulingStatus fromValue(String value) {
        SchedulingStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown SchedulingStatus: " + value);
        }
        return status;
    }
}