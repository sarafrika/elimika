package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.*;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
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
public interface AssessmentRubricRepository extends JpaRepository<AssessmentRubric, Long>, JpaSpecificationExecutor<AssessmentRubric> {
    Optional<AssessmentRubric> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    /**
     * Finds all public rubrics available for reuse.
     *
     * @param pageable pagination parameters
     * @return page of public rubrics
     */
    Page<AssessmentRubric> findByIsPublicTrueAndIsActiveTrueOrderByCreatedDateDesc(Pageable pageable);

    /**
     * Finds public rubrics by rubric type.
     *
     * @param rubricType the type of rubric to search for
     * @param pageable pagination parameters
     * @return page of public rubrics of the specified type
     */
    Page<AssessmentRubric> findByIsPublicTrueAndIsActiveTrueAndRubricTypeContainingIgnoreCaseOrderByCreatedDateDesc(
            String rubricType, Pageable pageable);

    /**
     * Searches public rubrics by title or description.
     *
     * @param searchTerm the search term to match against title or description
     * @param pageable pagination parameters
     * @return page of matching public rubrics
     */
    @Query("""
        SELECT ar FROM AssessmentRubric ar 
        WHERE ar.isPublic = true AND ar.isActive = true 
        AND (LOWER(ar.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
             OR LOWER(ar.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY ar.createdDate DESC
        """)
    Page<AssessmentRubric> findPublicRubricsBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds rubrics created by a specific instructor.
     *
     * @param instructorUuid the UUID of the instructor
     * @param pageable pagination parameters
     * @return page of rubrics created by the instructor
     */
    Page<AssessmentRubric> findByInstructorUuidAndIsActiveTrueOrderByCreatedDateDesc(UUID instructorUuid, Pageable pageable);

    /**
     * Finds rubrics created by an instructor that are available for sharing.
     *
     * @param instructorUuid the UUID of the instructor
     * @param includePrivate whether to include private rubrics
     * @param pageable pagination parameters
     * @return page of instructor's shareable rubrics
     */
    @Query("""
        SELECT ar FROM AssessmentRubric ar 
        WHERE ar.instructorUuid = :instructorUuid AND ar.isActive = true
        AND (:includePrivate = true OR ar.isPublic = true)
        ORDER BY ar.createdDate DESC
        """)
    Page<AssessmentRubric> findInstructorShareableRubrics(
            @Param("instructorUuid") UUID instructorUuid, 
            @Param("includePrivate") boolean includePrivate, 
            Pageable pageable);

    /**
     * Finds the most popular public rubrics based on usage across courses.
     *
     * @param pageable pagination parameters
     * @return page of popular public rubrics
     */
    @Query("""
        SELECT ar FROM AssessmentRubric ar 
        LEFT JOIN CourseRubricAssociation cra ON ar.uuid = cra.rubricUuid
        WHERE ar.isPublic = true AND ar.isActive = true
        GROUP BY ar.id, ar.uuid, ar.title, ar.description, ar.rubricType, 
                 ar.instructorUuid, ar.isPublic, ar.status, ar.isActive, ar.totalWeight, 
                 ar.weightUnit, ar.usesCustomLevels, 
                 ar.maxScore, ar.minPassingScore, ar.createdDate, ar.createdBy, 
                 ar.lastModifiedDate, ar.lastModifiedBy
        ORDER BY COUNT(cra.id) DESC, ar.createdDate DESC
        """)
    Page<AssessmentRubric> findPopularPublicRubrics(Pageable pageable);

    /**
     * Finds rubrics by status.
     *
     * @param status the status to filter by
     * @param pageable pagination parameters
     * @return page of rubrics with the specified status
     */
    Page<AssessmentRubric> findByStatusAndIsActiveTrueOrderByCreatedDateDesc(ContentStatus status, Pageable pageable);

    /**
     * Counts total public rubrics available for reuse.
     *
     * @return count of public rubrics
     */
    long countByIsPublicTrueAndIsActiveTrue();

    /**
     * Counts rubrics created by a specific instructor.
     *
     * @param instructorUuid the UUID of the instructor
     * @return count of instructor's rubrics
     */
    long countByInstructorUuidAndIsActiveTrue(UUID instructorUuid);
}