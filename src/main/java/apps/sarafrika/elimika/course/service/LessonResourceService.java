package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonResourceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing lesson resources.
 */
public interface LessonResourceService {

    /**
     * Creates a new lesson resource.
     *
     * @param lessonResourceDTO The DTO containing lesson resource details.
     * @return The created LessonResourceDTO.
     */
    @Transactional
    LessonResourceDTO createLessonResource(LessonResourceDTO lessonResourceDTO);

    /**
     * Retrieves a lesson resource by its UUID.
     *
     * @param uuid The UUID of the lesson resource.
     * @return The LessonResourceDTO representing the lesson resource.
     */
    @Transactional(readOnly = true)
    LessonResourceDTO getLessonResourceByUuid(UUID uuid);

    /**
     * Retrieves all lesson resources with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonResourceDTOs.
     */
    @Transactional(readOnly = true)
    Page<LessonResourceDTO> getAllLessonResources(Pageable pageable);

    /**
     * Updates an existing lesson resource.
     *
     * @param uuid The UUID of the lesson resource to update.
     * @param lessonResourceDTO The DTO containing updated lesson resource details.
     * @return The updated LessonResourceDTO.
     */
    @Transactional
    LessonResourceDTO updateLessonResource(UUID uuid, LessonResourceDTO lessonResourceDTO);

    /**
     * Deletes a lesson resource by UUID.
     *
     * @param uuid The UUID of the lesson resource to delete.
     */
    @Transactional
    void deleteLessonResource(UUID uuid);

    /**
     * Searches for lesson resources based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of LessonResourceDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<LessonResourceDTO> searchLessonResources(Map<String, String> searchParams, Pageable pageable);
}