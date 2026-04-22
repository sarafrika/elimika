package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle state for a marketplace class job.
 */
public enum ClassMarketplaceJobStatus {
    OPEN("open"),
    FILLED("filled"),
    CANCELLED("cancelled");

    private final String value;
    private static final Map<String, ClassMarketplaceJobStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ClassMarketplaceJobStatus status : values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    ClassMarketplaceJobStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ClassMarketplaceJobStatus fromValue(String value) {
        ClassMarketplaceJobStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ClassMarketplaceJobStatus: " + value);
        }
        return status;
    }

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED;
    }
}
