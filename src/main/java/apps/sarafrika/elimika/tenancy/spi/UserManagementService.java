package apps.sarafrika.elimika.tenancy.spi;

import org.keycloak.representations.idm.UserRepresentation;

/**
 * User Management Service Provider Interface
 * <p>
 * Provides user creation and synchronization operations for other modules.
 * This interface exposes essential user management functionality
 * without giving direct access to User entities, repositories, or internal services.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface UserManagementService {

    /**
     * Creates a new user from Keycloak representation.
     * This is typically called when a user authenticates for the first time
     * and needs to be synchronized to the local database.
     *
     * @param userRep The Keycloak user representation
     */
    void createUser(UserRepresentation userRep);

    /**
     * Ensures a user exists in the local database by synchronizing from Keycloak.
     * If the user doesn't exist locally, fetches their information from Keycloak
     * and creates a local record.
     *
     * @param keycloakUserId The Keycloak user ID
     * @param realm The Keycloak realm
     * @throws IllegalStateException if user exists in JWT but not in Keycloak
     * @throws RuntimeException if synchronization fails
     */
    void ensureUserExists(String keycloakUserId, String realm);
}