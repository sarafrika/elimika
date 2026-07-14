package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why a requested resource booking window cannot be satisfied.
 */
public enum ResourceConflictType {
    RESOURCE_INACTIVE("RESOURCE_INACTIVE", "Resource is deactivated"),
    OUTSIDE_OPEN_HOURS("OUTSIDE_OPEN_HOURS", "Window falls outside the resource's open hours"),
    BLACKOUT("BLACKOUT", "Window intersects a blackout rule"),
    CONFIRMED_BOOKING("CONFIRMED_BOOKING", "Window overlaps a confirmed booking"),
    ACTIVE_HOLD("ACTIVE_HOLD", "Window overlaps another marketplace job's recruitment hold"),
    INSUFFICIENT_QUANTITY("INSUFFICIENT_QUANTITY", "Equipment pool has insufficient remaining quantity"),
    VENUE_CAPACITY_EXCEEDED("VENUE_CAPACITY_EXCEEDED", "Requested participants exceed the venue seat capacity");

    private final String value;
    private final String description;
    private static final Map<String, ResourceConflictType> VALUE_MAP = new HashMap<>();

    static {
        for (ResourceConflictType type : ResourceConflictType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }

    ResourceConflictType(String value, String description) {
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
    public static ResourceConflictType fromValue(String value) {
        ResourceConflictType type = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown ResourceConflictType: " + value);
        }
        return type;
    }
}
