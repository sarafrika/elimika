package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseCategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing course categories.
 */
public interface CourseCategoryService {

    /**
     * Creates a new course category.
     *
     * @param courseCategoryDTO The DTO containing course category details.
     * @return The created CourseCategoryDTO.
     */
    @Transactional
    CourseCategoryDTO createCourseCategory(CourseCategoryDTO courseCategoryDTO);

    /**
     * Retrieves a course category by its UUID.
     *
     * @param uuid The UUID of the course category.
     * @return The CourseCategoryDTO representing the course category.
     */
    @Transactional(readOnly = true)
    CourseCategoryDTO getCourseCategoryByUuid(UUID uuid);

    /**
     * Retrieves all course categories with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseCategoryDTOs.
     */
    @Transactional(readOnly = true)
    Page<CourseCategoryDTO> getAllCourseCategories(Pageable pageable);

    /**
     * Updates an existing course category.
     *
     * @param uuid The UUID of the course category to update.
     * @param courseCategoryDTO The DTO containing updated course category details.
     * @return The updated CourseCategoryDTO.
     */
    @Transactional
    CourseCategoryDTO updateCourseCategory(UUID uuid, CourseCategoryDTO courseCategoryDTO);

    /**
     * Deletes a course category by UUID.
     *
     * @param uuid The UUID of the course category to delete.
     */
    @Transactional
    void deleteCourseCategory(UUID uuid);

    /**
     * Searches for course categories based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CourseCategoryDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<CourseCategoryDTO> searchCourseCategories(Map<String, String> searchParams, Pageable pageable);
}