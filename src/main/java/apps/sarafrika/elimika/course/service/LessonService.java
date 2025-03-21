package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing lessons.
 */
public interface LessonService {

    /**
     * Creates a new lesson.
     *
     * @param lessonDTO The DTO containing lesson details.
     * @return The created LessonDTO.
     */
    @Transactional
    LessonDTO createLesson(LessonDTO lessonDTO);

    /**
     * Retrieves a lesson by its UUID.
     *
     * @param uuid The UUID of the lesson.
     * @return The LessonDTO representing the lesson.
     */
    @Transactional(readOnly = true)
    LessonDTO getLessonByUuid(UUID uuid);

    /**
     * Retrieves all lessons with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonDTOs.
     */
    @Transactional(readOnly = true)
    Page<LessonDTO> getAllLessons(Pageable pageable);

    /**
     * Updates an existing lesson.
     *
     * @param uuid The UUID of the lesson to update.
     * @param lessonDTO The DTO containing updated lesson details.
     * @return The updated LessonDTO.
     */
    @Transactional
    LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO);

    /**
     * Deletes a lesson by UUID.
     *
     * @param uuid The UUID of the lesson to delete.
     */
    @Transactional
    void deleteLesson(UUID uuid);

    /**
     * Searches for lessons based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<LessonDTO> searchLessons(Map<String, String> searchParams, Pageable pageable);
}