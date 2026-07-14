package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Kind of bookable organisation resource.
 */
public enum ResourceType {
    VENUE("VENUE", "Physical space booked exclusively per time slot (classroom, lab)"),
    EQUIPMENT_POOL("EQUIPMENT_POOL", "Countable equipment booked by quantity (laptops, instruments)");

    private final String value;
    private final String description;
    private static final Map<String, ResourceType> VALUE_MAP = new HashMap<>();

    static {
        for (ResourceType type : ResourceType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }

    ResourceType(String value, String description) {
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
    public static ResourceType fromValue(String value) {
        ResourceType type = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown ResourceType: " + value);
        }
        return type;
    }
}
