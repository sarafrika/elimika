package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.common.exceptions.CategoryNotFoundException;
import apps.sarafrika.elimika.course.mappers.CategoryMapper;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.course.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GenericSpecificationBuilder<Category> specificationBuilder;

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        log.info("Creating new category with name: {}", categoryDTO.name());

        if (categoryRepository.existsByNameIgnoreCase(categoryDTO.name())) {
            log.error("Category with name '{}' already exists", categoryDTO.name());
            throw new DataIntegrityViolationException(CATEGORY_EXISTS);
        }

        Category category = CategoryMapper.toEntity(categoryDTO);
        Category savedCategory = categoryRepository.save(category);
        log.info(CATEGORY_CREATED_SUCCESS);

        return CategoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryByUuid(UUID uuid) {
        log.info("Fetching category with UUID: {}", uuid);

        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new CategoryNotFoundException(ERROR_CATEGORY_NOT_FOUND)
                );

        log.info(CATEGORY_FOUND_SUCCESS);
        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> getAllCategories(Pageable pageable) {
        log.info("Fetching all categories with pagination: {}", pageable);

        Page<Category> categories = categoryRepository.findAll(pageable);
        Page<CategoryDTO> categoryDTOs = categories.map(CategoryMapper::toDto);

        log.info("Found {} categories", categories.getTotalElements());
        return categoryDTOs;
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(UUID uuid, CategoryDTO categoryDTO) {
        log.info("Updating category with UUID: {}", uuid);

        Category existingCategory = categoryRepository.findByUuid(uuid)
                .orElseThrow(() ->
                        new CategoryNotFoundException(ERROR_CATEGORY_NOT_FOUND)
                );

        // Check if name is being changed and if it would conflict with an existing category
        if (categoryDTO.name() != null &&
                !existingCategory.getName().equalsIgnoreCase(categoryDTO.name()) &&
                categoryRepository.existsByNameIgnoreCase(categoryDTO.name())) {
            log.error("Cannot update category. A category with name '{}' already exists", categoryDTO.name());
            throw new DataIntegrityViolationException(CATEGORY_EXISTS);
        }

        // Directly update the fields instead of using a mapper
        if (categoryDTO.name() != null) {
            existingCategory.setName(categoryDTO.name());
        }

        if (categoryDTO.description() != null) {
            existingCategory.setDescription(categoryDTO.description());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        log.info(CATEGORY_UPDATED_SUCCESS);

        return CategoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID uuid) {
        log.info("Deleting category with UUID: {}", uuid);

        Category category = categoryRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("Category with UUID '{}' not found", uuid);
                    return new CategoryNotFoundException(ERROR_CATEGORY_NOT_FOUND);
                });

        categoryRepository.delete(category);
        log.info("Category with UUID '{}' has been deleted successfully", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> searchCategories(Map<String, String> searchParams, Pageable pageable) {
        log.info("Searching categories with parameters: {}", searchParams);

        Specification<Category> specification = specificationBuilder.buildSpecification(Category.class, searchParams);
        Page<Category> categories = categoryRepository.findAll(specification, pageable);

        return categories.map(CategoryMapper::toDto);
    }
}