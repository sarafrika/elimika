package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseRubricAssociationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing course-rubric associations
 * <p>
 * Provides business logic for associating rubrics with courses,
 * supporting rubric reuse across multiple courses.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
public interface CourseRubricAssociationService {

    /**
     * Associates a rubric with a course.
     *
     * @param associationDTO the association details
     * @return the created association
     */
    CourseRubricAssociationDTO associateRubricWithCourse(CourseRubricAssociationDTO associationDTO);

    /**
     * Removes the association between a rubric and a course.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     */
    void dissociateRubricFromCourse(UUID courseUuid, UUID rubricUuid);

    /**
     * Removes a specific association by context.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @param usageContext the usage context
     */
    void dissociateRubricFromCourseByContext(UUID courseUuid, UUID rubricUuid, String usageContext);

    /**
     * Gets all rubrics associated with a course.
     *
     * @param courseUuid the UUID of the course
     * @param pageable pagination parameters
     * @return page of associated rubrics
     */
    Page<CourseRubricAssociationDTO> getRubricsByCourse(UUID courseUuid, Pageable pageable);

    /**
     * Gets all courses that use a specific rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @param pageable pagination parameters
     * @return page of courses using the rubric
     */
    Page<CourseRubricAssociationDTO> getCoursesByRubric(UUID rubricUuid, Pageable pageable);

    /**
     * Gets the primary rubric for a course.
     *
     * @param courseUuid the UUID of the course
     * @return the primary rubric association, or null if none exists
     */
    CourseRubricAssociationDTO getPrimaryRubricForCourse(UUID courseUuid);

    /**
     * Sets a rubric as the primary rubric for a course.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @param instructorUuid the UUID of the instructor making the change
     * @return the updated primary association
     */
    CourseRubricAssociationDTO setPrimaryRubric(UUID courseUuid, UUID rubricUuid, UUID instructorUuid);

    /**
     * Gets associations by usage context.
     *
     * @param courseUuid the UUID of the course
     * @param usageContext the usage context
     * @param pageable pagination parameters
     * @return page of associations for the context
     */
    Page<CourseRubricAssociationDTO> getAssociationsByContext(UUID courseUuid, String usageContext, Pageable pageable);

    /**
     * Updates an association's context or primary status.
     *
     * @param associationUuid the UUID of the association
     * @param associationDTO the updated association details
     * @return the updated association
     */
    CourseRubricAssociationDTO updateAssociation(UUID associationUuid, CourseRubricAssociationDTO associationDTO);

    /**
     * Gets an association by its UUID.
     *
     * @param associationUuid the UUID of the association
     * @return the association details
     */
    CourseRubricAssociationDTO getAssociationByUuid(UUID associationUuid);

    /**
     * Checks if a rubric is associated with a course.
     *
     * @param courseUuid the UUID of the course
     * @param rubricUuid the UUID of the rubric
     * @return true if the association exists
     */
    boolean isRubricAssociatedWithCourse(UUID courseUuid, UUID rubricUuid);

    /**
     * Gets usage statistics for a rubric.
     *
     * @param rubricUuid the UUID of the rubric
     * @return count of courses using the rubric
     */
    long getRubricUsageCount(UUID rubricUuid);

    /**
     * Gets all associations created by an instructor.
     *
     * @param instructorUuid the UUID of the instructor
     * @param pageable pagination parameters
     * @return page of associations created by the instructor
     */
    Page<CourseRubricAssociationDTO> getAssociationsByInstructor(UUID instructorUuid, Pageable pageable);
}