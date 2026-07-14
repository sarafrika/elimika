package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What created a resource booking.
 */
public enum ResourceBookingSourceType {
    MARKETPLACE_JOB("MARKETPLACE_JOB", "Recruitment hold placed when a marketplace job was posted"),
    CLASS_DEFINITION("CLASS_DEFINITION", "Booking backing a scheduled class session"),
    MANUAL("MANUAL", "Manually created booking");

    private final String value;
    private final String description;
    private static final Map<String, ResourceBookingSourceType> VALUE_MAP = new HashMap<>();

    static {
        for (ResourceBookingSourceType type : ResourceBookingSourceType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }

    ResourceBookingSourceType(String value, String description) {
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
    public static ResourceBookingSourceType fromValue(String value) {
        ResourceBookingSourceType type = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown ResourceBookingSourceType: " + value);
        }
        return type;
    }
}
