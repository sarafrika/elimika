package apps.sarafrika.elimika.classes.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Lifecycle state for an instructor application against a marketplace class job.
 */
public enum ClassMarketplaceJobApplicationStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ASSIGNED("assigned"),
    NOT_SELECTED("not_selected");

    private final String value;
    private static final Map<String, ClassMarketplaceJobApplicationStatus> VALUE_MAP = new HashMap<>();

    static {
        for (ClassMarketplaceJobApplicationStatus status : values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toUpperCase(), status);
        }
    }

    ClassMarketplaceJobApplicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ClassMarketplaceJobApplicationStatus fromValue(String value) {
        ClassMarketplaceJobApplicationStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown ClassMarketplaceJobApplicationStatus: " + value);
        }
        return status;
    }

    public boolean isFinal() {
        return this == REJECTED || this == ASSIGNED || this == NOT_SELECTED;
    }
}
