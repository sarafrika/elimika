package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseCategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * @throws apps.sarafrika.elimika.common.exceptions.RecordNotFoundException if the course category is not found
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
     * @throws apps.sarafrika.elimika.common.exceptions.RecordNotFoundException if the course category is not found
     */
    @Transactional
    CourseCategoryDTO updateCourseCategory(UUID uuid, CourseCategoryDTO courseCategoryDTO);

    /**
     * Deletes a course category by UUID.
     *
     * @param uuid The UUID of the course category to delete.
     * @throws apps.sarafrika.elimika.common.exceptions.RecordNotFoundException if the course category is not found
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

    /**
     * Retrieves all categories associated with a specific course.
     *
     * @param courseUuid The UUID of the course.
     * @return A list of course category DTOs.
     */
    @Transactional(readOnly = true)
    List<CourseCategoryDTO> getCategoriesByCourseUuid(UUID courseUuid);

    /**
     * Saves a list of categories for a course.
     *
     * @param courseUuid The UUID of the course.
     * @param categories The list of category DTOs to associate with the course.
     */
    @Transactional
    void saveCourseCategories(UUID courseUuid, List<CourseCategoryDTO> categories);

    /**
     * Updates the category associations for a course.
     *
     * @param courseUuid The UUID of the course.
     * @param categories The updated list of category DTOs.
     */
    @Transactional
    void updateCourseCategories(UUID courseUuid, List<CourseCategoryDTO> categories);

    /**
     * Deletes all category associations for a specific course.
     *
     * @param courseUuid The UUID of the course whose category associations will be deleted.
     */
    @Transactional
    void deleteCategoriesByCourseUuid(UUID courseUuid);

    /**
     * Checks if a course has a specific category.
     *
     * @param courseUuid The UUID of the course.
     * @param categoryUuid The UUID of the category.
     * @return True if the course has the category, false otherwise.
     */
    @Transactional(readOnly = true)
    boolean hasCourseCategory(UUID courseUuid, UUID categoryUuid);
}