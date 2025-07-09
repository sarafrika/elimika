package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced service interface for managing users and their organization relationships.
 * <p>
 * Provides operations for user management including organization affiliations,
 * role assignments, and branch associations. Users can exist independently
 * or be affiliated with organizations/branches with specific roles.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-07-09
 */
public interface UserService {

    // ================================
    // CORE USER MANAGEMENT
    // ================================

    /**
     * Creates a new user from Keycloak representation.
     *
     * @param userRep the Keycloak user representation
     */
    void createUser(UserRepresentation userRep);

    /**
     * Retrieves a user by UUID with all their domains across organizations.
     *
     * @param uuid the user UUID
     * @return the user data with domains
     */
    UserDTO getUserByUuid(UUID uuid);

    /**
     * Updates user information and optionally their domains.
     * Note: Domain updates only apply to standalone users, not organization-specific roles.
     *
     * @param uuid the user UUID
     * @param userDTO the updated user data
     * @return the updated user
     */
    UserDTO updateUser(UUID uuid, UserDTO userDTO);

    /**
     * Uploads a profile image for a user.
     *
     * @param userUuid the user UUID
     * @param profileImage the profile image file
     * @return the updated user data
     */
    UserDTO uploadProfileImage(UUID userUuid, MultipartFile profileImage);

    /**
     * Deletes a user and all their organization relationships.
     *
     * @param uuid the user UUID
     */
    void deleteUser(UUID uuid);

    /**
     * Searches for users based on criteria.
     *
     * @param searchParams the search criteria
     * @param pageable pagination information
     * @return paginated search results
     */
    Page<UserDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ================================
    // ORGANIZATION AFFILIATION MANAGEMENT
    // ================================

    /**
     * Invites a user to an organization via email with a specific role.
     * Creates organization-user relationship with domain and optional branch assignment.
     *
     * @param email the user's email address
     * @param organisationUuid the organization UUID
     * @param domainName the user's role/domain in the organization
     * @param branchUuid optional branch assignment (can be null)
     * @return the updated user data
     * @throws IllegalArgumentException if domain is invalid or branch doesn't belong to organization
     */
    UserDTO inviteUserToOrganisation(String email, UUID organisationUuid, String domainName, UUID branchUuid);

    /**
     * Assigns an existing user to an organization with a specific role.
     * Updates existing relationship or creates new one.
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organization UUID
     * @param domainName the user's role/domain in the organization
     * @param branchUuid optional branch assignment (can be null)
     * @return the updated user data
     */
    UserDTO assignUserToOrganisation(UUID userUuid, UUID organisationUuid, String domainName, UUID branchUuid);

    /**
     * Removes a user from an organization (soft delete).
     * Ends the user-organization relationship while preserving history.
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organization UUID
     */
    void removeUserFromOrganisation(UUID userUuid, UUID organisationUuid);

    // ================================
    // ORGANIZATION USER QUERIES
    // ================================

    /**
     * Retrieves users belonging to a specific organization with pagination.
     *
     * @param organisationId the organization UUID
     * @param pageable pagination information
     * @return paginated list of users in the organization
     */
    Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable);

    /**
     * Retrieves users with a specific role in an organization.
     *
     * @param organisationUuid the organization UUID
     * @param domainName the role/domain name (student, instructor, admin, organisation_user)
     * @return list of users with the specified role
     */
    List<UserDTO> getUsersByOrganisationAndDomain(UUID organisationUuid, String domainName);

    /**
     * Retrieves users assigned to a specific training branch.
     *
     * @param branchUuid the branch UUID
     * @return list of users in the branch
     */
    List<UserDTO> getUsersByBranch(UUID branchUuid);

    // ================================
    // ROLE AND PERMISSION CHECKS
    // ================================

    /**
     * Checks if a user has a specific role in an organization.
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organization UUID
     * @param domainName the role/domain to check
     * @return true if user has the specified role in the organization
     */
    boolean hasUserRoleInOrganisation(UUID userUuid, UUID organisationUuid, String domainName);

    // ================================
    // USER DOMAIN MANAGEMENT
    // ================================

    /**
     * Gets all domains/roles for a user across all organizations.
     * For unaffiliated users: returns their standalone domains (excluding organisation_user)
     * For affiliated users: returns all their organization-specific domains
     *
     * @param userUuid the user UUID
     * @return list of domain names
     */
    List<String> getUserDomains(UUID userUuid);
}