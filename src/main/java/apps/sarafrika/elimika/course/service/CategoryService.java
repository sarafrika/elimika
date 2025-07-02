package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CategoryService {
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    CategoryDTO getCategoryByUuid(UUID uuid);
    Page<CategoryDTO> getAllCategories(Pageable pageable);
    CategoryDTO updateCategory(UUID uuid, CategoryDTO categoryDTO);
    void deleteCategory(UUID uuid);
    Page<CategoryDTO> search(Map<String, String> searchParams, Pageable pageable);
}
