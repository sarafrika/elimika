package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.RubricScoring;
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

@Repository
public interface RubricScoringRepository extends JpaRepository<RubricScoring, Long>,
        JpaSpecificationExecutor<RubricScoring> {
    Optional<RubricScoring> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Page<RubricScoring> findAllByCriteriaUuid(UUID criteriaUuid, Pageable pageable);

    Optional<RubricScoring> findByUuidAndCriteriaUuid(UUID uuid, UUID criteriaUuid);

    boolean existsByUuidAndCriteriaUuid(UUID uuid, UUID criteriaUuid);

    /**
     * Finds rubric scoring by criteria UUID and rubric scoring level UUID (for custom levels).
     *
     * @param criteriaUuid the UUID of the criteria
     * @param rubricScoringLevelUuid the UUID of the rubric scoring level
     * @return optional containing the rubric scoring if found
     */
    Optional<RubricScoring> findByCriteriaUuidAndRubricScoringLevelUuid(UUID criteriaUuid, UUID rubricScoringLevelUuid);

    /**
     * Finds all rubric scoring entries for a specific rubric (via criteria).
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of all scoring entries for the rubric
     */
    @Query("SELECT rs FROM RubricScoring rs JOIN RubricCriteria rc ON rs.criteriaUuid = rc.uuid WHERE rc.rubricUuid = :rubricUuid")
    List<RubricScoring> findByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Finds all matrix cells for a specific rubric ordered by criteria display order and level order.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of scoring entries ordered for matrix display
     */
    @Query("""
        SELECT rs FROM RubricScoring rs 
        JOIN RubricCriteria rc ON rs.criteriaUuid = rc.uuid 
        LEFT JOIN RubricScoringLevel rsl ON rs.rubricScoringLevelUuid = rsl.uuid
        WHERE rc.rubricUuid = :rubricUuid 
        ORDER BY rc.displayOrder ASC, rsl.levelOrder ASC
        """)
    List<RubricScoring> findMatrixCellsByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Counts the number of completed matrix cells for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return count of cells with descriptions
     */
    @Query("""
        SELECT COUNT(rs) FROM RubricScoring rs 
        JOIN RubricCriteria rc ON rs.criteriaUuid = rc.uuid 
        WHERE rc.rubricUuid = :rubricUuid AND rs.description IS NOT NULL AND rs.description != ''
        """)
    long countCompletedCellsByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Counts the total number of expected matrix cells for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return total expected cells (criteria count × scoring levels count)
     */
    @Query("""
        SELECT 
            (SELECT COUNT(rc) FROM RubricCriteria rc WHERE rc.rubricUuid = :rubricUuid) *
            (SELECT COUNT(rsl) FROM RubricScoringLevel rsl WHERE rsl.rubricUuid = :rubricUuid)
        """)
    long countExpectedCellsByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Checks if a matrix cell exists for given criteria and scoring level.
     *
     * @param criteriaUuid the UUID of the criteria
     * @param rubricScoringLevelUuid the UUID of the rubric scoring level (for custom levels)
     * @return true if the cell exists
     */
    @Query("""
        SELECT COUNT(rs) > 0 FROM RubricScoring rs 
        WHERE rs.criteriaUuid = :criteriaUuid 
        AND rs.rubricScoringLevelUuid = :rubricScoringLevelUuid
        """)
    boolean existsMatrixCell(@Param("criteriaUuid") UUID criteriaUuid,
                           @Param("rubricScoringLevelUuid") UUID rubricScoringLevelUuid);
}
