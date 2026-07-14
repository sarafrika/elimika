package apps.sarafrika.elimika.resourcing.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Kind of resource availability rule.
 * <p>
 * A resource with no OPEN_HOURS rules is considered open at all times; once any
 * OPEN_HOURS rule exists, bookings must fall fully inside an applicable open window.
 * BLACKOUT rules always exclude their window.
 */
public enum AvailabilityRuleType {
    OPEN_HOURS("OPEN_HOURS", "Recurring window during which the resource may be booked"),
    BLACKOUT("BLACKOUT", "Recurring or one-off window during which the resource is unavailable");

    private final String value;
    private final String description;
    private static final Map<String, AvailabilityRuleType> VALUE_MAP = new HashMap<>();

    static {
        for (AvailabilityRuleType type : AvailabilityRuleType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }

    AvailabilityRuleType(String value, String description) {
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
    public static AvailabilityRuleType fromValue(String value) {
        AvailabilityRuleType type = value == null ? null : VALUE_MAP.get(value.toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown AvailabilityRuleType: " + value);
        }
        return type;
    }
}
