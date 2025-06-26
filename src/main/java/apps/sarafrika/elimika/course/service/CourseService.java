package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.common.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Course Service Interface
 * <p>
 * Defines the contract for course management operations in the Sarafrika Elimika system.
 * This service handles all business logic related to course creation, modification, retrieval,
 * and management operations while working primarily with CourseDTO objects.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
public interface CourseService {

    /**
     * Creates a new course in the system.
     *
     * @param courseDTO the course data transfer object containing course information
     * @return the created course as a DTO with generated system fields
     * @throws IllegalArgumentException if courseDTO is null or contains invalid data
     * @throws DuplicateResourceException if course code already exists
     */
    CourseDTO createCourse(CourseDTO courseDTO);

    /**
     * Updates an existing course identified by its UUID.
     *
     * @param uuid the unique identifier of the course to update
     * @param courseDTO the updated course information
     * @return the updated course as a DTO
     * @throws ResourceNotFoundException if course with given UUID is not found
     * @throws IllegalArgumentException if courseDTO contains invalid data
     */
    CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO);

    /**
     * Retrieves a course by its unique identifier.
     *
     * @param uuid the unique identifier of the course
     * @return an Optional containing the course DTO if found, empty otherwise
     */
    Optional<CourseDTO> getCourseByUuid(UUID uuid);

    /**
     * Retrieves all courses with pagination support.
     *
     * @param pageable pagination and sorting information
     * @return a page of course DTOs
     */
    Page<CourseDTO> getAllCourses(Pageable pageable);

    /**
     * Searches courses based on multiple search criteria.
     * Supported search parameters:
     * - "name": search in course name
     * - "description": search in course description
     * - "code": search by course code
     * - "status": filter by course status
     * - "difficulty": filter by difficulty level
     * - "minPrice": minimum price filter
     * - "maxPrice": maximum price filter
     * - "minAge": minimum age eligibility
     * - "maxAge": maximum age eligibility
     * - "createdBy": filter by creator
     * - "term": global search term (searches name, description, and code)
     *
     * @param searchParams map containing search criteria as key-value pairs
     * @param pageable pagination and sorting information
     * @return a page of course DTOs matching the search criteria
     */
    Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Permanently deletes a course from the system.
     * This operation is irreversible and should be used with caution.
     *
     * @param uuid the unique identifier of the course to delete
     * @throws ResourceNotFoundException if course with given UUID is not found
     */
    void deleteCourse(UUID uuid);

    /**
     * Uploads a thumbnail image for a course.
     *
     * @param courseUuid the UUID of the course to update
     * @param thumbnailImage the thumbnail image file to upload
     * @return the updated course DTO with thumbnail URL
     * @throws ResourceNotFoundException if course with given UUID is not found
     * @throws IllegalArgumentException if thumbnailImage is invalid
     */
    CourseDTO uploadCourseThumbnail(UUID courseUuid, MultipartFile thumbnailImage);
}