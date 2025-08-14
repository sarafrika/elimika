package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RubricScoringLevel entities
 * <p>
 * Provides data access methods for managing rubric scoring levels including
 * custom query methods for matrix operations and level management.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Repository
public interface RubricScoringLevelRepository extends JpaRepository<RubricScoringLevel, Long>, JpaSpecificationExecutor<RubricScoringLevel> {

    /**
     * Finds a rubric scoring level by its UUID.
     *
     * @param uuid the UUID of the scoring level
     * @return optional containing the scoring level if found
     */
    Optional<RubricScoringLevel> findByUuid(UUID uuid);

    /**
     * Finds all scoring levels for a specific rubric ordered by level order.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of scoring levels ordered by level_order ASC
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid ORDER BY rsl.levelOrder ASC")
    List<RubricScoringLevel> findByRubricUuidOrderByLevelOrder(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Finds all scoring levels for a specific rubric ordered by level order with pagination.
     *
     * @param rubricUuid the UUID of the rubric
     * @param pageable pagination information
     * @return page of scoring levels ordered by level_order ASC
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid ORDER BY rsl.levelOrder ASC")
    Page<RubricScoringLevel> findByRubricUuidOrderByLevelOrder(@Param("rubricUuid") UUID rubricUuid, Pageable pageable);

    /**
     * Finds all scoring levels for a specific rubric with pagination.
     *
     * @param rubricUuid the UUID of the rubric
     * @param pageable pagination information
     * @return page of scoring levels
     */
    Page<RubricScoringLevel> findByRubricUuid(UUID rubricUuid, Pageable pageable);

    /**
     * Finds a scoring level by rubric UUID and level order.
     *
     * @param rubricUuid the UUID of the rubric
     * @param levelOrder the order of the level
     * @return optional containing the scoring level if found
     */
    Optional<RubricScoringLevel> findByRubricUuidAndLevelOrder(UUID rubricUuid, Integer levelOrder);

    /**
     * Finds a scoring level by rubric UUID and name.
     *
     * @param rubricUuid the UUID of the rubric
     * @param name the name of the scoring level
     * @return optional containing the scoring level if found
     */
    Optional<RubricScoringLevel> findByRubricUuidAndName(UUID rubricUuid, String name);

    /**
     * Finds all passing scoring levels for a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of passing scoring levels ordered by level_order ASC
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid AND rsl.isPassing = true ORDER BY rsl.levelOrder ASC")
    List<RubricScoringLevel> findPassingLevelsByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Finds all passing scoring levels for a specific rubric with pagination.
     *
     * @param rubricUuid the UUID of the rubric
     * @param pageable pagination information
     * @return page of passing scoring levels ordered by level_order ASC
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid AND rsl.isPassing = true ORDER BY rsl.levelOrder ASC")
    Page<RubricScoringLevel> findPassingLevelsByRubricUuid(@Param("rubricUuid") UUID rubricUuid, Pageable pageable);

    /**
     * Finds the highest scoring level (level_order = 1) for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return optional containing the highest scoring level if found
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid AND rsl.levelOrder = 1")
    Optional<RubricScoringLevel> findHighestLevelByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Counts the number of scoring levels for a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return count of scoring levels
     */
    long countByRubricUuid(UUID rubricUuid);

    /**
     * Counts the number of passing scoring levels for a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return count of passing scoring levels
     */
    @Query("SELECT COUNT(rsl) FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid AND rsl.isPassing = true")
    long countPassingLevelsByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Checks if a rubric scoring level exists by UUID.
     *
     * @param uuid the UUID to check
     * @return true if the scoring level exists
     */
    boolean existsByUuid(UUID uuid);

    /**
     * Checks if a scoring level exists with the given rubric UUID and level order.
     *
     * @param rubricUuid the UUID of the rubric
     * @param levelOrder the order of the level
     * @return true if the scoring level exists
     */
    boolean existsByRubricUuidAndLevelOrder(UUID rubricUuid, Integer levelOrder);

    /**
     * Checks if a scoring level exists with the given rubric UUID and name.
     *
     * @param rubricUuid the UUID of the rubric
     * @param name the name of the scoring level
     * @return true if the scoring level exists
     */
    boolean existsByRubricUuidAndName(UUID rubricUuid, String name);

    /**
     * Deletes a rubric scoring level by UUID.
     *
     * @param uuid the UUID of the scoring level to delete
     */
    void deleteByUuid(UUID uuid);

    /**
     * Deletes all scoring levels for a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     */
    void deleteByRubricUuid(UUID rubricUuid);

    /**
     * Finds the next available level order for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return the next available level order (max + 1)
     */
    @Query("SELECT COALESCE(MAX(rsl.levelOrder), 0) + 1 FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid")
    Integer findNextLevelOrderByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Finds scoring levels by rubric UUID and point range.
     *
     * @param rubricUuid the UUID of the rubric
     * @param minPoints minimum points (inclusive)
     * @param maxPoints maximum points (inclusive)
     * @return list of scoring levels within the point range
     */
    @Query("SELECT rsl FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid AND rsl.points BETWEEN :minPoints AND :maxPoints ORDER BY rsl.points DESC")
    List<RubricScoringLevel> findByRubricUuidAndPointsRange(@Param("rubricUuid") UUID rubricUuid, 
                                                           @Param("minPoints") java.math.BigDecimal minPoints, 
                                                           @Param("maxPoints") java.math.BigDecimal maxPoints);
}