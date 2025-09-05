package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Recurrence type for class scheduling patterns.
 * Must match the database constraint: CHECK (recurrence_type IN ('DAILY', 'WEEKLY', 'MONTHLY'))
 */
public enum RecurrenceType {
    DAILY("DAILY", "Daily recurrence pattern"),
    WEEKLY("WEEKLY", "Weekly recurrence pattern"),
    MONTHLY("MONTHLY", "Monthly recurrence pattern");
    
    private final String value;
    private final String description;
    private static final Map<String, RecurrenceType> VALUE_MAP = new HashMap<>();
    
    static {
        for (RecurrenceType type : RecurrenceType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }
    
    RecurrenceType(String value, String description) {
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
    
    @Override
    public String toString() {
        return value;
    }
    
    @JsonCreator
    public static RecurrenceType fromValue(String value) {
        RecurrenceType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown RecurrenceType: " + value);
        }
        return type;
    }
    
    public static RecurrenceType fromString(String value) {
        return fromValue(value);
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
    public static RecurrenceType fromDatabaseValue(String value) {
        return fromValue(value);
    }
}