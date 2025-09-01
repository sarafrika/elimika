package apps.sarafrika.elimika.notifications.api;

/**
 * Status of a notification delivery.
 */
public enum DeliveryStatus {
    QUEUED("Queued for delivery"),
    PENDING("Being processed"),
    DELIVERED("Successfully delivered"),
    FAILED("Delivery failed"),
    BLOCKED("Blocked by user preferences"),
    EXPIRED("Notification expired before delivery");
    
    private final String description;
    
    DeliveryStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}