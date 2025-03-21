package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.course.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    public static final String ERROR_CATEGORY_NOT_FOUND = "Category not found.";
    private static final String CATEGORY_FOUND_SUCCESS = "Category retrieved successfully.";
    private static final String CATEGORY_CREATED_SUCCESS = "Category has been persisted successfully.";
    private static final String CATEGORY_UPDATED_SUCCESS = "Category has been updated successfully.";
    private static final String CATEGORY_EXISTS = "Category already exists.";

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        return null;
    }

    @Override
    public CategoryDTO getCategoryByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        return null;
    }

    @Override
    public CategoryDTO updateCategory(UUID uuid, CategoryDTO categoryDTO) {
        return null;
    }

    @Override
    public void deleteCategory(UUID uuid) {

    }

    @Override
    public Page<CategoryDTO> searchCategories(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
