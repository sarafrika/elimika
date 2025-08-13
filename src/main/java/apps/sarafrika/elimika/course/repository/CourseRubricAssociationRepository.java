package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CourseRubricAssociation entities
 * <p>
 * Provides data access methods for managing course-rubric associations,
 * supporting rubric reuse across multiple courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Repository
public interface CourseRubricAssociationRepository extends JpaRepository<CourseRubricAssociation, Long>, JpaSpecificationExecutor<CourseRubricAssociation> {

    /**
     * Finds an association by its UUID.
     *
     * @param uuid the UUID to search for
     * @return optional containing the association if found
     */
    Optional<CourseRubricAssociation> findByUuid(UUID uuid);

    /**
     * Deletes an association by its UUID.
     *
     * @param uuid the UUID of the association to delete
     */
    void deleteByUuid(UUID uuid);

    /**
     * Checks if an association exists by UUID.
     *
     * @param uuid the UUID to check
     * @return true if the association exists
     */
    boolean existsByUuid(UUID uuid);

    /**
     * Finds all rubrics associated with a specific course.
     *
     * @param courseUuid the UUID of the course
     * @return list of associations for the course
     */
    List<CourseRubricAssociation> findByCourseUuid(UUID courseUuid);

    /**
     * Finds all courses that use a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return list of associations for the rubric
     */
    List<CourseRubricAssociation> findByRubricUuid(UUID rubricUuid);

    /**
     * Finds a specific course-rubric association.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @return optional containing the association if found
     */
    Optional<CourseRubricAssociation> findByCourseUuidAndRubricUuid(UUID courseUuid, UUID rubricUuid);

    /**
     * Finds a specific course-rubric association with usage context.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @param usageContext the usage context
     * @return optional containing the association if found
     */
    Optional<CourseRubricAssociation> findByCourseUuidAndRubricUuidAndUsageContext(
            UUID courseUuid, UUID rubricUuid, String usageContext);

    /**
     * Finds the primary rubric for a course.
     *
     * @param courseUuid the UUID of the course
     * @return optional containing the primary rubric association
     */
    Optional<CourseRubricAssociation> findByCourseUuidAndIsPrimaryRubricTrue(UUID courseUuid);

    /**
     * Finds all associations created by a specific instructor.
     *
     * @param instructorUuid the UUID of the instructor
     * @return list of associations created by the instructor
     */
    List<CourseRubricAssociation> findByAssociatedBy(UUID instructorUuid);

    /**
     * Checks if a course-rubric association already exists.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @return true if the association exists
     */
    boolean existsByCourseUuidAndRubricUuid(UUID courseUuid, UUID rubricUuid);

    /**
     * Checks if a course-rubric association exists with a specific context.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @param usageContext the usage context
     * @return true if the association exists
     */
    boolean existsByCourseUuidAndRubricUuidAndUsageContext(UUID courseUuid, UUID rubricUuid, String usageContext);

    /**
     * Counts how many courses use a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return count of courses using the rubric
     */
    @Query("SELECT COUNT(DISTINCT cra.courseUuid) FROM CourseRubricAssociation cra WHERE cra.rubricUuid = :rubricUuid")
    long countCoursesByRubricUuid(@Param("rubricUuid") UUID rubricUuid);

    /**
     * Counts how many rubrics are used by a specific course.
     *
     * @param courseUuid the UUID of the course
     * @return count of rubrics used by the course
     */
    @Query("SELECT COUNT(DISTINCT cra.rubricUuid) FROM CourseRubricAssociation cra WHERE cra.courseUuid = :courseUuid")
    long countRubricsByCourseUuid(@Param("courseUuid") UUID courseUuid);

    /**
     * Finds associations by usage context across all courses.
     *
     * @param usageContext the usage context to search for
     * @return list of associations with the specified context
     */
    List<CourseRubricAssociation> findByUsageContext(String usageContext);
}