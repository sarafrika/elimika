package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Provides aggregated analytics for the commerce purchase module.
 */
public interface CommerceAnalyticsService {

    CommerceAnalyticsSnapshot captureSnapshot();
}