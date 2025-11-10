package apps.sarafrika.elimika.tenancy.spi;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Lookup Service Provider Interface
 * <p>
 * Provides read-only access to user and domain information for other modules.
 * This interface exposes essential user data needed by other modules
 * without giving direct access to User entities or repositories.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
public interface UserLookupService {

    /**
     * Finds a user by their Keycloak subject ID.
     *
     * @param keycloakId The Keycloak subject ID
     * @return Optional containing the user UUID, or empty if not found
     */
    Optional<UUID> findUserUuidByKeycloakId(String keycloakId);

    /**
     * Finds a user by their email address.
     *
     * @param email The email address
     * @return Optional containing the user UUID, or empty if not found
     */
    Optional<UUID> findUserUuidByEmail(String email);

    /**
     * Checks if a user exists.
     *
     * @param userUuid The UUID of the user
     * @return true if the user exists, false otherwise
     */
    boolean userExists(UUID userUuid);

    /**
     * Gets the email address of a user.
     *
     * @param userUuid The UUID of the user
     * @return Optional containing the email, or empty if user not found
     */
    Optional<String> getUserEmail(UUID userUuid);

    /**
     * Gets the full name of a user.
     *
     * @param userUuid The UUID of the user
     * @return Optional containing the full name, or empty if user not found
     */
    Optional<String> getUserFullName(UUID userUuid);

    /**
     * Gets the date of birth sourced from the profile (Keycloak/user record).
     *
     * @param userUuid User identifier
     * @return Optional date of birth
     */
    Optional<LocalDate> getUserDateOfBirth(UUID userUuid);

    /**
     * Checks if a user has a specific domain.
     *
     * @param userUuid The UUID of the user
     * @param domain The domain to check
     * @return true if the user has the domain, false otherwise
     */
    boolean userHasDomain(UUID userUuid, UserDomain domain);

    /**
     * Gets all domains for a user.
     *
     * @param userUuid The UUID of the user
     * @return List of domains (empty if user not found or has no domains)
     */
    List<UserDomain> getUserDomains(UUID userUuid);

    /**
     * Checks if a user has any of the specified domains.
     *
     * @param userUuid The UUID of the user
     * @param domains The domains to check
     * @return true if the user has any of the domains, false otherwise
     */
    boolean userHasAnyDomain(UUID userUuid, UserDomain... domains);

    /**
     * Gets the organization UUIDs for a user.
     *
     * @param userUuid The UUID of the user
     * @return List of organization UUIDs (empty if user not found or not in any organization)
     */
    List<UUID> getUserOrganizations(UUID userUuid);

    /**
     * Checks if a user belongs to a specific organization.
     *
     * @param userUuid The UUID of the user
     * @param organizationUuid The UUID of the organization
     * @return true if the user belongs to the organization, false otherwise
     */
    boolean userBelongsToOrganization(UUID userUuid, UUID organizationUuid);

    /**
     * Checks if a user exists by their Keycloak ID.
     *
     * @param keycloakId The Keycloak subject ID
     * @return true if the user exists, false otherwise
     */
    boolean existsByKeycloakId(String keycloakId);
}
