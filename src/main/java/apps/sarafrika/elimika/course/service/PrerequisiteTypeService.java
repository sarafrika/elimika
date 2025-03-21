package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.PrerequisiteTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing prerequisite types.
 */
public interface PrerequisiteTypeService {

    /**
     * Creates a new prerequisite type.
     *
     * @param prerequisiteTypeDTO The DTO containing prerequisite type details.
     * @return The created PrerequisiteTypeDTO.
     */
    @Transactional
    PrerequisiteTypeDTO createPrerequisiteType(PrerequisiteTypeDTO prerequisiteTypeDTO);

    /**
     * Retrieves a prerequisite type by its UUID.
     *
     * @param uuid The UUID of the prerequisite type.
     * @return The PrerequisiteTypeDTO representing the prerequisite type.
     */
    @Transactional(readOnly = true)
    PrerequisiteTypeDTO getPrerequisiteTypeByUuid(UUID uuid);

    /**
     * Retrieves all prerequisite types with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of PrerequisiteTypeDTOs.
     */
    @Transactional(readOnly = true)
    Page<PrerequisiteTypeDTO> getAllPrerequisiteTypes(Pageable pageable);

    /**
     * Updates an existing prerequisite type.
     *
     * @param uuid The UUID of the prerequisite type to update.
     * @param prerequisiteTypeDTO The DTO containing updated prerequisite type details.
     * @return The updated PrerequisiteTypeDTO.
     */
    @Transactional
    PrerequisiteTypeDTO updatePrerequisiteType(UUID uuid, PrerequisiteTypeDTO prerequisiteTypeDTO);

    /**
     * Deletes a prerequisite type by UUID.
     *
     * @param uuid The UUID of the prerequisite type to delete.
     */
    @Transactional
    void deletePrerequisiteType(UUID uuid);

    /**
     * Searches for prerequisite types based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of PrerequisiteTypeDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<PrerequisiteTypeDTO> searchPrerequisiteTypes(Map<String, String> searchParams, Pageable pageable);
}