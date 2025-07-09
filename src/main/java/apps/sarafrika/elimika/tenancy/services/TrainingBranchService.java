package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing training branches.
 * <p>
 * Provides operations for creating, reading, updating, and deleting training branches
 * within organisations in the Sarafrika Elimika system.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-07-09
 */
public interface TrainingBranchService {

    /**
     * Creates a new training branch.
     *
     * @param trainingBranchDTO the training branch data to create
     * @return the created training branch
     * @throws IllegalArgumentException if validation fails
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
     * Deletes a training branch (soft delete).
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
}