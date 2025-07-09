package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TrainingBranch entity.
 * <p>
 * Provides data access operations for training branches including
 * standard CRUD operations and custom query methods.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-07-09
 */
@Repository
public interface TrainingBranchRepository extends JpaRepository<TrainingBranch, Long>, JpaSpecificationExecutor<TrainingBranch> {

    /**
     * Finds a training branch by UUID, excluding soft-deleted records.
     *
     * @param uuid the UUID of the training branch
     * @return Optional containing the training branch if found and not deleted
     */
    Optional<TrainingBranch> findByUuidAndDeletedFalse(UUID uuid);

    /**
     * Finds all training branches excluding soft-deleted records with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of training branches
     */
    Page<TrainingBranch> findByDeletedFalse(Pageable pageable);

    /**
     * Finds all training branches for a specific organisation, excluding soft-deleted records.
     *
     * @param organisationUuid the UUID of the organisation
     * @param pageable pagination information
     * @return paginated list of training branches for the organisation
     */
    Page<TrainingBranch> findByOrganisationUuidAndDeletedFalse(UUID organisationUuid, Pageable pageable);

    /**
     * Checks if a training branch exists with the given organisation UUID and branch name,
     * excluding soft-deleted records.
     *
     * @param organisationUuid the UUID of the organisation
     * @param branchName the name of the branch
     * @return true if a training branch exists with these criteria
     */
    boolean existsByOrganisationUuidAndBranchNameAndDeletedFalse(UUID organisationUuid, String branchName);

    /**
     * Finds all training branches by organisation UUID, excluding soft-deleted records.
     * Used for cases where pagination is not needed.
     *
     * @param organisationUuid the UUID of the organisation
     * @return list of training branches for the organisation
     */
    List<TrainingBranch> findByOrganisationUuidAndDeletedFalse(UUID organisationUuid);

    Optional<TrainingBranch> findByUuid(UUID uuid);
}