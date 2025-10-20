package apps.sarafrika.elimika.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum AvailabilityType {
    DAILY("daily", "Daily recurring availability"),
    WEEKLY("weekly", "Weekly recurring availability"),
    MONTHLY("monthly", "Monthly recurring availability"),
    CUSTOM("custom", "Custom recurring pattern");
    
    private final String value;
    private final String description;
    private static final Map<String, AvailabilityType> VALUE_MAP = new HashMap<>();
    
    static {
        for (AvailabilityType type : AvailabilityType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }
    
    AvailabilityType(String value, String description) {
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
    public static AvailabilityType fromValue(String value) {
        AvailabilityType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown AvailabilityType: " + value);
        }
        return type;
    }
}