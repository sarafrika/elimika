package apps.sarafrika.elimika.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Location type for class delivery format.
 * Must match the database constraint: CHECK (location_type IN ('ONLINE', 'IN_PERSON', 'HYBRID'))
 */
public enum LocationType {
    ONLINE("ONLINE", "Online class delivery"),
    IN_PERSON("IN_PERSON", "In-person class delivery"),
    HYBRID("HYBRID", "Hybrid online and in-person class delivery");
    
    private final String value;
    private final String description;
    private static final Map<String, LocationType> VALUE_MAP = new HashMap<>();
    
    static {
        for (LocationType type : LocationType.values()) {
            VALUE_MAP.put(type.value, type);
        }
    }
    
    LocationType(String value, String description) {
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
    public static LocationType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("location_type is required");
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        LocationType type = VALUE_MAP.get(normalizedValue);
        if (type == null) {
            throw new IllegalArgumentException("Unknown LocationType: " + value);
        }
        return type;
    }
    
    public static LocationType fromString(String value) {
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
    public static LocationType fromDatabaseValue(String value) {
        return fromValue(value);
    }
}
