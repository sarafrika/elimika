package apps.sarafrika.elimika.authentication.spi;

import apps.sarafrika.elimika.shared.event.user.UserCreationEvent;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Optional;

/**
 * Service Provider Interface for Keycloak user management operations.
 * This interface is exposed via Spring Modulith's named interface pattern (spi)
 * to allow other modules to interact with Keycloak user operations.
 *
 * <p>Only essential operations used across the application are included.
 * Unused operations have been removed to maintain a clean API surface.</p>
 *
 * @author Wilfred Njuguna
 * @version 2.0
 * @since 2025-10-20
 */
public interface KeycloakUserService {

    /**
     * Creates a new user in Keycloak from a user creation event.
     *
     * @param newUserRecord the user creation event containing user details
     * @return the created user representation
     */
    UserRepresentation createUser(UserCreationEvent newUserRecord);

    /**
     * Retrieves a user from Keycloak by their ID.
     *
     * @param userId the Keycloak user ID
     * @param realm the Keycloak realm
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<UserRepresentation> getUserById(String userId, String realm);

    /**
     * Retrieves a user from Keycloak by their username.
     *
     * @param username the username to search for
     * @param realm the Keycloak realm
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<UserRepresentation> getUserByUsername(String username, String realm);

    /**
     * Updates an existing user in Keycloak.
     *
     * @param userId the Keycloak user ID
     * @param userRepresentation the updated user representation
     * @param realm the Keycloak realm
     */
    void updateUser(String userId, UserRepresentation userRepresentation, String realm);

    /**
     * Sends required action emails to a user (e.g., VERIFY_EMAIL, UPDATE_PASSWORD).
     *
     * @param userId the Keycloak user ID
     * @param actions list of required actions to send
     * @param realm the Keycloak realm
     */
    void sendRequiredActionEmail(String userId, List<String> actions, String realm);

    /**
     * Logs out a user from Keycloak, invalidating all their sessions.
     *
     * @param userId the Keycloak user ID
     * @param realm the Keycloak realm
     */
    void logoutUser(String userId, String realm);
}