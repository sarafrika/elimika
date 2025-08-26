package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced service interface for managing training branches and their user relationships.
 * <p>
 * Provides operations for training branch management including user assignments,
 * role management within branches, and point of contact management. Training branches
 * are sub-units within organizations that can have specific user assignments.
 *
 * @author Elimika Team
 * @version 2.0
 * @since 2025-07-09
 */
public interface TrainingBranchService {

    // ================================
    // CORE TRAINING BRANCH MANAGEMENT
    // ================================

    /**
     * Creates a new training branch within an organization.
     *
     * @param trainingBranchDTO the training branch data to create
     * @return the created training branch
     * @throws IllegalArgumentException if validation fails or branch name already exists in organization
     * @throws RuntimeException if creation fails
     */
    TrainingBranchDTO createTrainingBranch(TrainingBranchDTO trainingBranchDTO);

    /**
     * Retrieves a training branch by its UUID.
     *
     * @param uuid the UUID of the training branch
     * @return the training branch data
     * @throws ResourceNotFoundException if training branch is not found
     */
    TrainingBranchDTO getTrainingBranchByUuid(UUID uuid);

    /**
     * Retrieves all training branches with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of training branches
     */
    Page<TrainingBranchDTO> getAllTrainingBranches(Pageable pageable);

    /**
     * Retrieves all training branches for a specific organisation with pagination.
     *
     * @param organisationUuid the UUID of the organisation
     * @param pageable pagination information
     * @return paginated list of training branches for the organisation
     */
    Page<TrainingBranchDTO> getTrainingBranchesByOrganisation(UUID organisationUuid, Pageable pageable);

    /**
     * Updates an existing training branch.
     *
     * @param uuid the UUID of the training branch to update
     * @param trainingBranchDTO the updated training branch data
     * @return the updated training branch
     * @throws ResourceNotFoundException if training branch is not found
     * @throws RuntimeException if update fails
     */
    TrainingBranchDTO updateTrainingBranch(UUID uuid, TrainingBranchDTO trainingBranchDTO);

    /**
     * Deletes a training branch (soft delete) and removes all user assignments.
     * Users remain in the parent organization but lose branch-specific assignments.
     *
     * @param uuid the UUID of the training branch to delete
     * @throws ResourceNotFoundException if training branch is not found
     * @throws RuntimeException if deletion fails
     */
    void deleteTrainingBranch(UUID uuid);

    /**
     * Searches for training branches based on provided criteria.
     *
     * @param searchParams the search criteria
     * @param pageable pagination information
     * @return paginated search results
     */
    Page<TrainingBranchDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ================================
    // USER-BRANCH ASSIGNMENT MANAGEMENT
    // ================================

    /**
     * Assigns a user to a specific training branch with a role.
     * If user is not in the parent organization, creates organization membership first.
     * If user is already in organization, updates their branch assignment.
     *
     * @param branchUuid the training branch UUID
     * @param userUuid the user UUID
     * @param domainName the role/domain for the user in this branch
     * @throws IllegalArgumentException if domain is invalid
     */
    void assignUserToBranch(UUID branchUuid, UUID userUuid, String domainName);

    /**
     * Removes a user from a training branch.
     * User remains in the parent organization but loses branch-specific assignment.
     *
     * @param branchUuid the training branch UUID
     * @param userUuid the user UUID
     * @throws ResourceNotFoundException if user is not assigned to the branch
     */
    void removeUserFromBranch(UUID branchUuid, UUID userUuid);

    // ================================
    // BRANCH USER QUERIES
    // ================================

    /**
     * Gets all users assigned to a specific training branch.
     *
     * @param branchUuid the training branch UUID
     * @return list of users assigned to the branch
     */
    List<UserDTO> getBranchUsers(UUID branchUuid);

    /**
     * Gets users in a training branch filtered by role/domain.
     *
     * @param branchUuid the training branch UUID
     * @param domainName the role/domain to filter by
     * @return list of users with the specified role in the branch
     */
    List<UserDTO> getBranchUsersByDomain(UUID branchUuid, String domainName);

    /**
     * Gets all training branches a user is assigned to.
     *
     * @param userUuid the user UUID
     * @return list of training branches the user is assigned to
     */
    List<TrainingBranchDTO> getUserBranches(UUID userUuid);

    /**
     * Checks if a user is assigned to a specific training branch.
     *
     * @param branchUuid the training branch UUID
     * @param userUuid the user UUID
     * @return true if user is assigned to the branch
     */
    boolean isUserInBranch(UUID branchUuid, UUID userUuid);

}