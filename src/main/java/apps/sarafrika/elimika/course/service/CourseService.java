package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing courses.
 */
public interface CourseService {

    /**
     * Creates a new course.
     *
     * @param courseDTO The DTO containing course details.
     * @return The created CourseDTO.
     */
    @Transactional
    CourseDTO createCourse(CourseDTO courseDTO);

    /**
     * Retrieves a course by its UUID.
     *
     * @param uuid The UUID of the course.
     * @return The CourseDTO representing the course.
     */
    @Transactional(readOnly = true)
    CourseDTO getCourseByUuid(UUID uuid);

    /**
     * Retrieves all courses with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseDTOs.
     */
    @Transactional(readOnly = true)
    Page<CourseDTO> getAllCourses(Pageable pageable);

    /**
     * Updates an existing course.
     *
     * @param uuid The UUID of the course to update.
     * @param courseDTO The DTO containing updated course details.
     * @return The updated CourseDTO.
     */
    @Transactional
    CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO);

    /**
     * Deletes a course by UUID.
     *
     * @param uuid The UUID of the course to delete.
     */
    @Transactional
    void deleteCourse(UUID uuid);

    /**
     * Searches for courses based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<CourseDTO> searchCourses(Map<String, String> searchParams, Pageable pageable);
}