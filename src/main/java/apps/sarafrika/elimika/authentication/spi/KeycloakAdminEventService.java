package apps.sarafrika.elimika.authentication.spi;

/**
 * Provides access to Keycloak admin event telemetry so that other modules
 * can surface admin activity metrics without coupling to the KC client.
 */
public interface KeycloakAdminEventService {

    /**
     * Retrieves a summarized view of the most recent Keycloak admin events.
     *
     * @return snapshot of 24h/7d activity as reported by Keycloak admin events.
     */
    KeycloakAdminEventSummary getAdminEventSummary();
}
