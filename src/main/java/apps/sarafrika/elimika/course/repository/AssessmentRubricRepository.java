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
     * @param searchTerm the lower-cased, LIKE-escaped search term (see {@code LikePatterns#escapeLower})
     *                   to match against title or description
     * @param pageable pagination parameters
     * @return page of matching public rubrics
     */
    @Query("""
        SELECT ar FROM AssessmentRubric ar 
        WHERE ar.isPublic = true AND ar.isActive = true 
        AND (LOWER(ar.title) LIKE CONCAT('%', :searchTerm, '%') ESCAPE '\\'
             OR LOWER(ar.description) LIKE CONCAT('%', :searchTerm, '%') ESCAPE '\\')
        ORDER BY ar.createdDate DESC
        """)
    Page<AssessmentRubric> findPublicRubricsBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
        SELECT ar FROM AssessmentRubric ar
        WHERE ar.isPublic = true AND ar.isActive = true
        AND LOWER(ar.rubricType) LIKE CONCAT('%', :rubricType, '%') ESCAPE '\\'
        AND (LOWER(ar.title) LIKE CONCAT('%', :searchTerm, '%') ESCAPE '\\'
             OR LOWER(ar.description) LIKE CONCAT('%', :searchTerm, '%') ESCAPE '\\')
        ORDER BY ar.createdDate DESC
        """)
    Page<AssessmentRubric> findPublicRubricsBySearchTermAndType(@Param("searchTerm") String searchTerm,
                                                                @Param("rubricType") String rubricType,
                                                                Pageable pageable);

    /**
     * Finds rubrics created by a specific course creator.
     *
     * @param courseCreatorUuid the UUID of the course creator
     * @param pageable pagination parameters
     * @return page of rubrics created by the course creator
     */
    Page<AssessmentRubric> findByCourseCreatorUuidAndIsActiveTrueOrderByCreatedDateDesc(UUID courseCreatorUuid, Pageable pageable);

    /**
     * Finds rubrics created by a course creator that are available for sharing.
     *
     * @param courseCreatorUuid the UUID of the course creator
     * @param includePrivate whether to include private rubrics
     * @param pageable pagination parameters
     * @return page of the course creator's shareable rubrics
     */
    @Query("""
        SELECT ar FROM AssessmentRubric ar 
        WHERE ar.courseCreatorUuid = :courseCreatorUuid AND ar.isActive = true
        AND (:includePrivate = true OR ar.isPublic = true)
        ORDER BY ar.createdDate DESC
        """)
    Page<AssessmentRubric> findCourseCreatorShareableRubrics(
            @Param("courseCreatorUuid") UUID courseCreatorUuid, 
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
                 ar.courseCreatorUuid, ar.isPublic, ar.status, ar.isActive, ar.totalWeight, 
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

    Page<AssessmentRubric> findByStatusAndIsPublicTrueAndIsActiveTrueOrderByCreatedDateDesc(ContentStatus status, Pageable pageable);

    /**
     * Counts total public rubrics available for reuse.
     *
     * @return count of public rubrics
     */
    long countByIsPublicTrueAndIsActiveTrue();

    /**
     * Counts rubrics created by a specific course creator.
     *
     * @param courseCreatorUuid the UUID of the course creator
     * @return count of the course creator's rubrics
     */
    long countByCourseCreatorUuidAndIsActiveTrue(UUID courseCreatorUuid);

    /**
     * The courses that use a rubric: through a course-rubric association, a course assessment or one
     * of its line items, or an assignment or quiz in one of the course's lessons.
     *
     * @param rubricUuid the rubric
     * @return the UUIDs of the courses using it
     */
    @Query(value = """
            SELECT course_uuid FROM course_rubric_associations WHERE rubric_uuid = :rubricUuid
            UNION
            SELECT course_uuid FROM course_assessments WHERE rubric_uuid = :rubricUuid
            UNION
            SELECT ca.course_uuid FROM course_assessment_line_items li
                JOIN course_assessments ca ON ca.uuid = li.course_assessment_uuid
                WHERE li.rubric_uuid = :rubricUuid
            UNION
            SELECT l.course_uuid FROM assignments a
                JOIN lessons l ON l.uuid = a.lesson_uuid
                WHERE a.rubric_uuid = :rubricUuid
            UNION
            SELECT l.course_uuid FROM quizzes q
                JOIN lessons l ON l.uuid = q.lesson_uuid
                WHERE q.rubric_uuid = :rubricUuid
            """, nativeQuery = true)
    List<UUID> findCourseUuidsUsingRubric(@Param("rubricUuid") UUID rubricUuid);
}
