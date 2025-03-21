package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.PrerequisiteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing prerequisites.
 */
public interface PrerequisiteService {

    /**
     * Creates a new prerequisite.
     *
     * @param prerequisiteDTO The DTO containing prerequisite details.
     * @return The created PrerequisiteDTO.
     */
    @Transactional
    PrerequisiteDTO createPrerequisite(PrerequisiteDTO prerequisiteDTO);

    /**
     * Retrieves a prerequisite by its UUID.
     *
     * @param uuid The UUID of the prerequisite.
     * @return The PrerequisiteDTO representing the prerequisite.
     */
    @Transactional(readOnly = true)
    PrerequisiteDTO getPrerequisiteByUuid(UUID uuid);

    /**
     * Retrieves all prerequisites with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of PrerequisiteDTOs.
     */
    @Transactional(readOnly = true)
    Page<PrerequisiteDTO> getAllPrerequisites(Pageable pageable);

    /**
     * Updates an existing prerequisite.
     *
     * @param uuid The UUID of the prerequisite to update.
     * @param prerequisiteDTO The DTO containing updated prerequisite details.
     * @return The updated PrerequisiteDTO.
     */
    @Transactional
    PrerequisiteDTO updatePrerequisite(UUID uuid, PrerequisiteDTO prerequisiteDTO);

    /**
     * Deletes a prerequisite by UUID.
     *
     * @param uuid The UUID of the prerequisite to delete.
     */
    @Transactional
    void deletePrerequisite(UUID uuid);

    /**
     * Searches for prerequisites based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of PrerequisiteDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<PrerequisiteDTO> searchPrerequisites(Map<String, String> searchParams, Pageable pageable);
}