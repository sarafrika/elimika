package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced service interface for managing organizations and their user relationships.
 * <p>
 * Provides operations for organization management including user invitations,
 * role management, and member queries. Supports training center operations
 * with branch-specific user assignments.
 *
 * @author Wilfred Njuguna
 * @version 2.0
 * @since 2025-07-09
 */
public interface OrganisationService {

    // ================================
    // CORE ORGANIZATION MANAGEMENT
    // ================================

    /**
     * Creates a new organization.
     *
     * @param organisationDTO the organization data to create
     * @return the created organization
     */
    OrganisationDTO createOrganisation(OrganisationDTO organisationDTO);

    /**
     * Creates a new organization with a specified creator.
     * The creator is automatically assigned as organisation_user.
     *
     * @param organisationDTO the organization data to create
     * @param creatorUuid the UUID of the user creating the organization
     * @return the created organization
     */
    OrganisationDTO createOrganisation(OrganisationDTO organisationDTO, UUID creatorUuid);

    /**
     * Retrieves an organization by UUID.
     *
     * @param uuid the organization UUID
     * @return the organization data
     */
    OrganisationDTO getOrganisationByUuid(UUID uuid);

    /**
     * Retrieves all organizations with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of organizations
     */
    Page<OrganisationDTO> getAllOrganisations(Pageable pageable);

    /**
     * Updates an existing organization.
     *
     * @param uuid the organization UUID
     * @param organisationDTO the updated organization data
     * @return the updated organization
     */
    OrganisationDTO updateOrganisation(UUID uuid, OrganisationDTO organisationDTO);

    /**
     * Deletes an organization (soft delete) and all user relationships.
     *
     * @param uuid the organization UUID
     */
    void deleteOrganisation(UUID uuid);

    /**
     * Searches for organizations based on criteria.
     *
     * @param searchParams the search criteria
     * @param pageable pagination information
     * @return paginated search results
     */
    Page<OrganisationDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ================================
    // USER INVITATION AND MANAGEMENT
    // ================================

    /**
     * Invites a user to the organization via email with a specific role.
     * Creates a new user-organization relationship.
     *
     * @param organisationUuid the organization UUID
     * @param email the user's email address
     * @param domainName the role/domain for the user (student, instructor, admin, organisation_user)
     * @param branchUuid optional training branch assignment (can be null)
     * @throws IllegalStateException if user is already associated with the organization
     * @throws IllegalArgumentException if branch doesn't belong to organization
     */
    void inviteUserToOrganisation(UUID organisationUuid, String email, String domainName, UUID branchUuid);

    /**
     * Removes a user from the organization (soft delete).
     * Ends the user-organization relationship while preserving history.
     *
     * @param organisationUuid the organization UUID
     * @param userUuid the user UUID
     */
    void removeUserFromOrganisation(UUID organisationUuid, UUID userUuid);

    /**
     * Updates a user's role within the organization.
     *
     * @param organisationUuid the organization UUID
     * @param userUuid the user UUID
     * @param newDomainName the new role/domain name
     */
    void updateUserRoleInOrganisation(UUID organisationUuid, UUID userUuid, String newDomainName);

    // ================================
    // ORGANIZATION USER QUERIES
    // ================================

    /**
     * Gets all users in the organization with their roles.
     *
     * @param organisationUuid the organization UUID
     * @return list of users in the organization
     */
    List<UserDTO> getOrganisationUsers(UUID organisationUuid);

    /**
     * Gets users in the organization filtered by role/domain.
     *
     * @param organisationUuid the organization UUID
     * @param domainName the role/domain to filter by
     * @return list of users with the specified role
     */
    List<UserDTO> getOrganisationUsersByDomain(UUID organisationUuid, String domainName);

    // ================================
    // USER-CENTRIC ORGANIZATION QUERIES
    // ================================

    /**
     * Gets all organizations a user belongs to.
     *
     * @param userUuid the user UUID
     * @return list of organizations the user is affiliated with
     */
    List<OrganisationDTO> getUserOrganisations(UUID userUuid);

    /**
     * Checks if a user is a member of the organization.
     *
     * @param organisationUuid the organization UUID
     * @param userUuid the user UUID
     * @return true if user is a member of the organization
     */
    boolean isUserInOrganisation(UUID organisationUuid, UUID userUuid);

    /**
     * Gets the user's role/domain in the organization.
     *
     * @param organisationUuid the organization UUID
     * @param userUuid the user UUID
     * @return the user's role/domain name in the organization
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if user is not associated with organization
     */
    String getUserRoleInOrganisation(UUID organisationUuid, UUID userUuid);
}