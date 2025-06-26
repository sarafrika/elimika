package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.common.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.util.enums.LessonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lesson Service Interface
 * <p>
 * Defines the contract for lesson management operations in the Sarafrika Elimika system.
 * This service handles all business logic related to lesson creation, modification, retrieval,
 * and management operations while working primarily with LessonDTO objects.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
public interface LessonService {

    /**
     * Creates a new lesson in the system.
     *
     * @param lessonDTO the lesson data transfer object containing lesson information
     * @return the created lesson as a DTO with generated system fields
     * @throws IllegalArgumentException if lessonDTO is null or contains invalid data
     * @throws ResourceNotFoundException if the associated course is not found
     * @throws DuplicateResourceException if lesson number already exists for the course
     */
    LessonDTO createLesson(LessonDTO lessonDTO);

    /**
     * Updates an existing lesson identified by its UUID.
     *
     * @param uuid the unique identifier of the lesson to update
     * @param lessonDTO the updated lesson information
     * @return the updated lesson as a DTO
     * @throws ResourceNotFoundException if lesson with given UUID is not found
     * @throws IllegalArgumentException if lessonDTO contains invalid data
     */
    LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO);

    /**
     * Retrieves a lesson by its unique identifier.
     *
     * @param uuid the unique identifier of the lesson
     * @return an Optional containing the lesson DTO if found, empty otherwise
     */
    Optional<LessonDTO> getLessonByUuid(UUID uuid);

    /**
     * Retrieves all lessons for a specific course ordered by lesson number.
     *
     * @param courseUuid the UUID of the course
     * @return a list of lesson DTOs ordered by lesson number
     * @throws ResourceNotFoundException if course with given UUID is not found
     */
    List<LessonDTO> getLessonsByCourse(UUID courseUuid);

    /**
     * Retrieves all lessons for a specific course with pagination support.
     *
     * @param courseUuid the UUID of the course
     * @param pageable pagination and sorting information
     * @return a page of lesson DTOs for the specified course
     * @throws ResourceNotFoundException if course with given UUID is not found
     */
    Page<LessonDTO> getLessonsByCourse(UUID courseUuid, Pageable pageable);

    /**
     * Retrieves lessons by type with pagination support.
     *
     * @param lessonType the type of lessons to retrieve
     * @param pageable pagination and sorting information
     * @return a page of lesson DTOs matching the specified type
     */
    Page<LessonDTO> getLessonsByType(LessonType lessonType, Pageable pageable);

    /**
     * Searches lessons based on multiple search criteria.
     * Supported search parameters:
     * - "name": search in lesson name
     * - "description": search in lesson description
     * - "courseUuid": filter by course UUID
     * - "type": filter by lesson type
     * - "minDuration": minimum duration in minutes
     * - "maxDuration": maximum duration in minutes
     * - "lessonNo": filter by lesson number
     * - "createdBy": filter by creator
     * - "term": global search term (searches name and description)
     *
     * @param searchParams map containing search criteria as key-value pairs
     * @param pageable pagination and sorting information
     * @return a page of lesson DTOs matching the search criteria
     */
    Page<LessonDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Updates the lesson number of a specific lesson.
     * This operation may require reordering other lessons in the course.
     *
     * @param uuid the unique identifier of the lesson
     * @param newLessonNo the new lesson number
     * @return the updated lesson DTO
     * @throws ResourceNotFoundException if lesson with given UUID is not found
     * @throws IllegalArgumentException if newLessonNo is invalid
     * @throws DuplicateResourceException if newLessonNo already exists for the course
     */
    LessonDTO updateLessonNumber(UUID uuid, Integer newLessonNo);


    /**
     * Deletes a lesson from the system.
     * This operation will automatically reorder remaining lessons in the course.
     *
     * @param uuid the unique identifier of the lesson to delete
     * @throws ResourceNotFoundException if lesson with given UUID is not found
     */
    void deleteLesson(UUID uuid);

    /**
     * Bulk deletes multiple lessons from the system.
     *
     * @param lessonUuids the list of lesson UUIDs to delete
     * @throws ResourceNotFoundException if any of the lessons are not found
     */
    void bulkDeleteLessons(List<UUID> lessonUuids);

    /**
     * Checks if a lesson exists by its UUID.
     *
     * @param uuid the unique identifier to check
     * @return true if the lesson exists, false otherwise
     */
    boolean existsByUuid(UUID uuid);

    /**
     * Checks if a lesson number already exists for a specific course.
     *
     * @param courseUuid the UUID of the course
     * @param lessonNo the lesson number to check
     * @return true if the lesson number exists for the course, false otherwise
     */
    boolean existsByLessonNoAndCourse(UUID courseUuid, Integer lessonNo);
}