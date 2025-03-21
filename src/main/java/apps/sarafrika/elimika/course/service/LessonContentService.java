package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing lesson content.
 */
public interface LessonContentService {

    /**
     * Creates a new lesson content.
     *
     * @param lessonContentDTO The DTO containing lesson content details.
     * @return The created LessonContentDTO.
     */
    @Transactional
    LessonContentDTO createLessonContent(LessonContentDTO lessonContentDTO);

    /**
     * Retrieves a lesson content by its UUID.
     *
     * @param uuid The UUID of the lesson content.
     * @return The LessonContentDTO representing the lesson content.
     */
    @Transactional(readOnly = true)
    LessonContentDTO getLessonContentByUuid(UUID uuid);

    /**
     * Retrieves all lesson contents with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonContentDTOs.
     */
    @Transactional(readOnly = true)
    Page<LessonContentDTO> getAllLessonContents(Pageable pageable);

    /**
     * Updates an existing lesson content.
     *
     * @param uuid The UUID of the lesson content to update.
     * @param lessonContentDTO The DTO containing updated lesson content details.
     * @return The updated LessonContentDTO.
     */
    @Transactional
    LessonContentDTO updateLessonContent(UUID uuid, LessonContentDTO lessonContentDTO);

    /**
     * Deletes a lesson content by UUID.
     *
     * @param uuid The UUID of the lesson content to delete.
     */
    @Transactional
    void deleteLessonContent(UUID uuid);

    /**
     * Searches for lesson contents based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonContentDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<LessonContentDTO> searchLessonContents(Map<String, String> searchParams, Pageable pageable);
}