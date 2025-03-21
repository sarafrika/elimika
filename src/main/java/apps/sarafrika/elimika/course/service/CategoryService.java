package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing course categories.
 */
public interface CategoryService {

    /**
     * Creates a new category.
     *
     * @param categoryDTO The DTO containing category details.
     * @return The created CategoryDTO.
     */
    @Transactional
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    /**
     * Retrieves a category by its UUID.
     *
     * @param uuid The UUID of the category.
     * @return The CategoryDTO representing the category.
     */
    @Transactional(readOnly = true)
    CategoryDTO getCategoryByUuid(UUID uuid);

    /**
     * Retrieves all categories with pagination.
     *
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CategoryDTOs.
     */
    @Transactional(readOnly = true)
    Page<CategoryDTO> getAllCategories(Pageable pageable);

    /**
     * Updates an existing category.
     *
     * @param uuid The UUID of the category to update.
     * @param categoryDTO The DTO containing updated category details.
     * @return The updated CategoryDTO.
     */
    CategoryDTO updateCategory(UUID uuid, CategoryDTO categoryDTO);

    /**
     * Deletes a category by UUID.
     *
     * @param uuid The UUID of the category to delete.
     */
    @Transactional
    void deleteCategory(UUID uuid);

    /**
     * Searches for categories based on given parameters.
     *
     * @param searchParams A map containing search filters.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of CategoryDTOs matching the search criteria.
     */
    @Transactional(readOnly = true)
    Page<CategoryDTO> searchCategories(Map<String, String> searchParams, Pageable pageable);
}