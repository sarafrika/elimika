package apps.sarafrika.elimika.shared.tracking.discovery;

import java.util.Locale;

/**
 * What happened to a recommended item. Impressions are recorded by the recommender when it answers;
 * clicks and dismissals are reported by the client.
 */
public enum DiscoveryEventType {
    IMPRESSION,
    CLICK,
    DISMISS;

    /** Case-insensitive lookup, so legacy or client-cased values keep working. */
    public static DiscoveryEventType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Discovery event type is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown discovery event type: " + value, ex);
        }
    }
}
