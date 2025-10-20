package apps.sarafrika.elimika.authentication.services;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Keycloak role management operations.
 * This is an internal service used within the authentication module.
 *
 * <p>Only essential operations used across the application are included.
 * Unused operations have been removed to maintain a clean API surface.</p>
 *
 * @author Wilfred Njuguna
 * @version 2.0
 * @since 2025-10-20
 */
public interface KeycloakRoleService {

    /**
     * Creates a new role in Keycloak.
     *
     * @param name the role name
     * @param description the role description
     * @param realm the Keycloak realm
     * @return the created role representation
     */
    RoleRepresentation createRole(String name, String description, String realm);

    /**
     * Retrieves a role by its name.
     *
     * @param name the role name
     * @param realm the Keycloak realm
     * @return Optional containing the role if found, empty otherwise
     */
    Optional<RoleRepresentation> getRoleByName(String name, String realm);

    /**
     * Retrieves all roles in a realm.
     *
     * @param realm the Keycloak realm
     * @return list of all roles
     */
    List<RoleRepresentation> getAllRoles(String realm);

    /**
     * Assigns a role to a user, removing all other roles first.
     *
     * @param userId the Keycloak user ID
     * @param roleName the role name to assign
     * @param realm the Keycloak realm
     */
    void assignRoleToUser(String userId, String roleName, String realm);

    /**
     * Retrieves all roles assigned to a user.
     *
     * @param userId the Keycloak user ID
     * @param realm the Keycloak realm
     * @return list of user's roles
     */
    List<RoleRepresentation> getUserRoles(String userId, String realm);
}
