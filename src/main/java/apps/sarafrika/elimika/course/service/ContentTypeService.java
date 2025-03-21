package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing content types.
 */
public interface ContentTypeService {

    /**
     * Creates a new content type.
     *
     * @param contentTypeDTO The DTO containing content type details.
     * @return The created ContentTypeDTO.
     */
    @Transactional
    ContentTypeDTO createContentType(ContentTypeDTO contentTypeDTO);

    /**
     * Retrieves a content type by its UUID.
     *
     * @param uuid The UUID of the content type.
     * @return The ContentTypeDTO representing the content type.
     */
    @Transactional(readOnly = true)
    ContentTypeDTO getContentTypeByUuid(UUID uuid);

    /**
     * Retrieves all content types with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of ContentTypeDTOs.
     */
    @Transactional(readOnly = true)
    Page<ContentTypeDTO> getAllContentTypes(Pageable pageable);

    /**
     * Updates an existing content type.
     *
     * @param uuid The UUID of the content type to update.
     * @param contentTypeDTO The DTO containing updated content type details.
     * @return The updated ContentTypeDTO.
     */
    @Transactional
    ContentTypeDTO updateContentType(UUID uuid, ContentTypeDTO contentTypeDTO);

    /**
     * Deletes a content type by UUID.
     *
     * @param uuid The UUID of the content type to delete.
     */
    @Transactional
    void deleteContentType(UUID uuid);

    /**
     * Searches for content types based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of ContentTypeDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<ContentTypeDTO> searchContentTypes(Map<String, String> searchParams, Pageable pageable);
}