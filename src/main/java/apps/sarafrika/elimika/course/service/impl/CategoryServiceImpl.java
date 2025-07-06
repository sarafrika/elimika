package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.course.factory.CategoryFactory;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    private final GenericSpecificationBuilder<Category> specificationBuilder;

    private static final String CATEGORY_NOT_FOUND_TEMPLATE = "Category with ID %s not found";

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = CategoryFactory.toEntity(categoryDTO);

        // Set defaults - leveraging DTO's isRootCategory() logic
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        Category savedCategory = categoryRepository.save(category);
        return CategoryFactory.toDTO(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryByUuid(UUID uuid) {
        return categoryRepository.findByUuid(uuid)
                .map(CategoryFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CATEGORY_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(CategoryFactory::toDTO);
    }

    @Override
    public CategoryDTO updateCategory(UUID uuid, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CATEGORY_NOT_FOUND_TEMPLATE, uuid)));

        updateCategoryFields(existingCategory, categoryDTO);

        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryFactory.toDTO(updatedCategory);
    }

    @Override
    public void deleteCategory(UUID uuid) {
        if (!categoryRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(CATEGORY_NOT_FOUND_TEMPLATE, uuid));
        }
        categoryRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Category> spec = specificationBuilder.buildSpecification(
                Category.class, searchParams);
        return categoryRepository.findAll(spec, pageable).map(CategoryFactory::toDTO);
    }

    // Domain-specific methods leveraging CategoryDTO computed properties
    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        return categoryRepository.findByParentUuidIsNull()
                .stream()
                .map(CategoryFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubCategories(UUID parentUuid) {
        return categoryRepository.findByParentUuid(parentUuid)
                .stream()
                .map(CategoryFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue()
                .stream()
                .map(CategoryFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryHierarchy(UUID categoryUuid) {
        // This would build the full path using CategoryDTO.getCategoryPath()
        List<Category> hierarchy = categoryRepository.findCategoryHierarchy(categoryUuid);
        return hierarchy.stream()
                .map(CategoryFactory::toDTO)
                .collect(Collectors.toList());
    }

    public boolean hasSubCategories(UUID categoryUuid) {
        return categoryRepository.countByParentUuid(categoryUuid) > 0;
    }

    public boolean canDeleteCategory(UUID categoryUuid) {
        // Check if category has subcategories or courses
        return !hasSubCategories(categoryUuid) &&
                courseRepository.countByCategoryUuid(categoryUuid) == 0;
    }

    private void updateCategoryFields(Category existingCategory, CategoryDTO dto) {
        if (dto.name() != null) {
            existingCategory.setName(dto.name());
        }
        if (dto.description() != null) {
            existingCategory.setDescription(dto.description());
        }
        if (dto.parentUuid() != null) {
            existingCategory.setParentUuid(dto.parentUuid());
        }
        if (dto.isActive() != null) {
            existingCategory.setIsActive(dto.isActive());
        }
    }
}