package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing course-related operations.
 * Defines the business logic for creating, retrieving, updating,
 * and deleting courses, as well as searching courses based on various criteria.
 */
public interface CourseService {
    /**
     * Creates a new course with the provided data.
     * This operation is transactional and will rollback if any exception occurs.
     *
     * @param courseDTO the course data to create
     * @return the created course with all associated data
     * @throws org.springframework.dao.DataIntegrityViolationException if a course with the same name already exists
     */
    @Transactional
    CourseDTO createCourse(CourseDTO courseDTO);

    /**
     * Retrieves a course by its UUID.
     * This is a read-only operation that doesn't modify any data.
     *
     * @param uuid the UUID of the course to retrieve
     * @return the found course with all associated data
     * @throws apps.sarafrika.elimika.common.exceptions.CourseNotFoundException if the course is not found
     */
    @Transactional(readOnly = true)
    CourseDTO getCourseByUuid(UUID uuid);

    /**
     * Retrieves all courses with pagination.
     * This is a read-only operation that doesn't modify any data.
     *
     * @param pageable pagination information including page number, size, and sorting
     * @return a page of courses with their associated data
     */
    @Transactional(readOnly = true)
    Page<CourseDTO> getAllCourses(Pageable pageable);

    /**
     * Updates an existing course with the provided data.
     * This operation is transactional and will rollback if any exception occurs.
     *
     * @param uuid the UUID of the course to update
     * @param courseDTO the new course data
     * @return the updated course with all associated data
     * @throws apps.sarafrika.elimika.common.exceptions.CourseNotFoundException if the course is not found
     * @throws org.springframework.dao.DataIntegrityViolationException if the update would result in a name conflict
     */
    @Transactional
    CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO);

    /**
     * Deletes a course by its UUID.
     * This operation is transactional and will rollback if any exception occurs.
     * All associated data (categories, learning objectives, etc.) will also be deleted.
     *
     * @param uuid the UUID of the course to delete
     * @throws apps.sarafrika.elimika.common.exceptions.CourseNotFoundException if the course is not found
     */
    @Transactional
    void deleteCourse(UUID uuid);

    /**
     * Searches for courses based on provided search parameters.
     * This is a read-only operation that doesn't modify any data.
     *
     * @param searchParams map of search parameters (name, description, difficulty level, price range, etc.)
     * @param pageable pagination information including page number, size, and sorting
     * @return a page of matching courses with their associated data
     */
    @Transactional(readOnly = true)
    Page<CourseDTO> searchCourses(Map<String, String> searchParams, Pageable pageable);
}