package apps.sarafrika.elimika.notifications.api;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Options for customizing notification delivery behavior.
 */
public record DeliveryOptions(
    Set<String> forceChannels,      // Override user preferences for specific channels
    boolean respectQuietHours,      // Whether to respect user's quiet hours
    boolean bypassPreferences,      // Completely bypass user preferences (admin only)
    LocalDateTime deliverAt,        // Schedule delivery for specific time
    int maxRetries                  // Maximum retry attempts for failed deliveries
) {
    
    public static DeliveryOptions defaults() {
        return new DeliveryOptions(
            Set.of(),
            true,
            false,
            null,
            3
        );
    }
    
    public static DeliveryOptions immediate() {
        return new DeliveryOptions(
            Set.of(),
            false,
            false,
            null,
            1
        );
    }
    
    public static DeliveryOptions scheduled(LocalDateTime deliveryTime) {
        return new DeliveryOptions(
            Set.of(),
            true,
            false,
            deliveryTime,
            3
        );
    }
    
    public static DeliveryOptions forceEmail() {
        return new DeliveryOptions(
            Set.of("email"),
            true,
            false,
            null,
            3
        );
    }
}