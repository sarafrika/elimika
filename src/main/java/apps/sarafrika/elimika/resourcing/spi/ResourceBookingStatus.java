package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lifecycle state of a resource booking. Only HOLD and CONFIRMED occupy the resource.
 */
public enum ResourceBookingStatus {
    HOLD("HOLD", "Tentative reservation while a marketplace job recruits"),
    CONFIRMED("CONFIRMED", "Firm reservation backing a scheduled class session"),
    RELEASED("RELEASED", "Reservation released (job cancelled, expired, or occurrence not scheduled)"),
    CANCELLED("CANCELLED", "Reservation cancelled along with its scheduled session");

    private final String value;
    private final String description;
    private static final Map<String, ResourceBookingStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ResourceBookingStatus status : ResourceBookingStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }

    ResourceBookingStatus(String value, String description) {
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
    public static ResourceBookingStatus fromValue(String value) {
        ResourceBookingStatus status = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (status == null) {
            throw new IllegalArgumentException("Unknown ResourceBookingStatus: " + value);
        }
        return status;
    }
}
