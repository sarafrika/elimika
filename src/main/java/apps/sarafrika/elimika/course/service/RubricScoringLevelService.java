package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.RubricScoringLevelDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing rubric scoring levels
 * <p>
 * Provides business logic operations for managing custom scoring levels
 * within rubrics for flexible matrix configurations.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
public interface RubricScoringLevelService {

    /**
     * Creates a new rubric scoring level for the specified rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @param rubricScoringLevelDTO the scoring level data
     * @return the created scoring level DTO
     */
    RubricScoringLevelDTO createRubricScoringLevel(UUID rubricUuid, RubricScoringLevelDTO rubricScoringLevelDTO);

    /**
     * Retrieves a rubric scoring level by UUID.
     *
     * @param uuid the UUID of the scoring level
     * @return the scoring level DTO
     */
    RubricScoringLevelDTO getRubricScoringLevelByUuid(UUID uuid);

    /**
     * Retrieves all scoring levels for a specific rubric ordered by level order.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of scoring levels ordered by level_order ASC
     */
    List<RubricScoringLevelDTO> getScoringLevelsByRubricUuid(UUID rubricUuid);

    /**
     * Retrieves all scoring levels for a specific rubric with pagination.
     *
     * @param rubricUuid the UUID of the rubric
     * @param pageable pagination information
     * @return page of scoring levels
     */
    Page<RubricScoringLevelDTO> getScoringLevelsByRubricUuid(UUID rubricUuid, Pageable pageable);

    /**
     * Retrieves all scoring levels with pagination.
     *
     * @param pageable pagination information
     * @return page of all scoring levels
     */
    Page<RubricScoringLevelDTO> getAllRubricScoringLevels(Pageable pageable);

    /**
     * Updates an existing rubric scoring level.
     *
     * @param rubricUuid the UUID of the rubric
     * @param levelUuid the UUID of the scoring level
     * @param rubricScoringLevelDTO the updated scoring level data
     * @return the updated scoring level DTO
     */
    RubricScoringLevelDTO updateRubricScoringLevel(UUID rubricUuid, UUID levelUuid, RubricScoringLevelDTO rubricScoringLevelDTO);

    /**
     * Deletes a rubric scoring level.
     *
     * @param rubricUuid the UUID of the rubric
     * @param levelUuid the UUID of the scoring level
     */
    void deleteRubricScoringLevel(UUID rubricUuid, UUID levelUuid);

    /**
     * Searches for scoring levels based on provided parameters.
     *
     * @param searchParams search parameters
     * @param pageable pagination information
     * @return page of matching scoring levels
     */
    Page<RubricScoringLevelDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Retrieves passing scoring levels for a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of passing scoring levels
     */
    List<RubricScoringLevelDTO> getPassingScoringLevels(UUID rubricUuid);

    /**
     * Retrieves the highest scoring level for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return the highest scoring level DTO or null if not found
     */
    RubricScoringLevelDTO getHighestScoringLevel(UUID rubricUuid);

    /**
     * Reorders scoring levels for a rubric by updating their level_order values.
     *
     * @param rubricUuid the UUID of the rubric
     * @param levelOrderMap map of scoring level UUIDs to their new order values
     */
    void reorderScoringLevels(UUID rubricUuid, Map<UUID, Integer> levelOrderMap);

    /**
     * Creates default scoring levels for a rubric based on the specified template.
     *
     * @param rubricUuid the UUID of the rubric
     * @param template the template to use (standard, simple, advanced)
     * @param createdBy the user creating the levels
     * @return list of created scoring level DTOs
     */
    List<RubricScoringLevelDTO> createDefaultScoringLevels(UUID rubricUuid, String template, String createdBy);
}