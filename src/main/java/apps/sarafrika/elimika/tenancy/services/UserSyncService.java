package apps.sarafrika.elimika.tenancy.services;

/**
 * Service for synchronizing users from Keycloak to local database.
 * This service ensures that authenticated users exist in the local database
 * by fetching their information from Keycloak and creating local records as needed.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
public interface UserSyncService {

    /**
     * Ensures a user exists in the local database by synchronizing from Keycloak.
     * If the user doesn't exist locally, fetches their information from Keycloak
     * and creates a local record.
     *
     * @param keycloakUserId the Keycloak user ID
     * @param realm the Keycloak realm
     * @throws IllegalStateException if user exists in JWT but not in Keycloak
     * @throws RuntimeException if synchronization fails
     */
    void ensureUserExists(String keycloakUserId, String realm);

    /**
     * Checks if a user exists in the local database.
     *
     * @param keycloakUserId the Keycloak user ID
     * @return true if user exists locally, false otherwise
     */
    boolean userExists(String keycloakUserId);
}
