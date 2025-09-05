package apps.sarafrika.elimika.notifications.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Status of a notification delivery.
 * Must match the database constraint: CHECK (delivery_status IN ('QUEUED', 'PENDING', 'DELIVERED', 'FAILED', 'BLOCKED', 'EXPIRED'))
 */
public enum DeliveryStatus {
    QUEUED("QUEUED", "Queued for delivery"),
    PENDING("PENDING", "Being processed"),
    DELIVERED("DELIVERED", "Successfully delivered"),
    FAILED("FAILED", "Delivery failed"),
    BLOCKED("BLOCKED", "Blocked by user preferences"),
    EXPIRED("EXPIRED", "Notification expired before delivery");
    
    private final String value;
    private final String description;
    private static final Map<String, DeliveryStatus> VALUE_MAP = new HashMap<>();
    
    static {
        for (DeliveryStatus status : DeliveryStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }
    
    DeliveryStatus(String value, String description) {
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
    public static DeliveryStatus fromValue(String value) {
        DeliveryStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown DeliveryStatus: " + value);
        }
        return status;
    }
    
    public static DeliveryStatus fromString(String value) {
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
    public static DeliveryStatus fromDatabaseValue(String value) {
        return fromValue(value);
    }
}