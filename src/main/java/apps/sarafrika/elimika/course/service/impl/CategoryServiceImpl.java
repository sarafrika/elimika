package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CategoryNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.request.CreateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.course.persistence.Category;
import apps.sarafrika.elimika.course.persistence.CategoryFactory;
import apps.sarafrika.elimika.course.persistence.CategoryRepository;
import apps.sarafrika.elimika.course.persistence.CategorySpecification;
import apps.sarafrika.elimika.course.service.CategoryService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<CategoryResponseDTO> findAllCategories(CategoryRequestDTO categoryRequestDTO, Pageable pageable) {

        Specification<Category> specification = new CategorySpecification(categoryRequestDTO);

        Page<Category> categories = categoryRepository.findAll(specification, pageable);

        List<CategoryResponseDTO> categoryResponseDTOs = categories.stream()
                .map(CategoryResponseDTO::from)
                .toList();

        return new ResponsePageableDTO<>(categoryResponseDTOs, categories.getNumber(), categories.getSize(),
                categories.getTotalPages(), categories.getTotalElements(), HttpStatus.OK.value(), CATEGORY_FOUND_SUCCESS);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<CategoryResponseDTO> findCategory(Long id) {

        Category category = findCategoryById(id);

        return new ResponseDTO<>(CategoryResponseDTO.from(category), HttpStatus.OK.value(), CATEGORY_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<List<CategoryResponseDTO>> findCategoriesByIds(List<Long> ids) {

        List<Category> categories = findCategoriesByAllIds(ids);

        List<CategoryResponseDTO> foundCategories = categories.stream()
                .map(CategoryResponseDTO::from)
                .toList();

        return new ResponseDTO<>(foundCategories, HttpStatus.OK.value(), CATEGORY_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Category findCategoryById(Long id) {

        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(ERROR_CATEGORY_NOT_FOUND));
    }

    private List<Category> findCategoriesByAllIds(List<Long> ids) {

        return categoryRepository.findByIdIn(ids);
    }

    @Transactional
    @Override
    public ResponseDTO<CategoryResponseDTO> createCategory(CreateCategoryRequestDTO createCategoryRequestDTO) {

        Optional<Category> existingCategory = categoryRepository.findByName(createCategoryRequestDTO.name());

        if (existingCategory.isPresent()) {

            return new ResponseDTO<>(CategoryResponseDTO.from(existingCategory.get()), HttpStatus.OK.value(), CATEGORY_EXISTS, null, LocalDateTime.now());
        }

        Category category = CategoryFactory.create(createCategoryRequestDTO);

        categoryRepository.save(category);

        return new ResponseDTO<>(CategoryResponseDTO.from(category), HttpStatus.CREATED.value(), CATEGORY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<List<CategoryResponseDTO>> createCategories(List<CreateCategoryRequestDTO> createCategoryRequestDTOS) {

        List<Category> categories = createCategoryRequestDTOS.stream()
                .map(createCategoryRequestDTO -> {
                    return categoryRepository.findByName(createCategoryRequestDTO.name().trim().toLowerCase()).orElseGet(() -> CategoryFactory.create(createCategoryRequestDTO));
                })
                .toList();


        categoryRepository.saveAll(categories);

        return new ResponseDTO<>(categories.stream().map(CategoryResponseDTO::from).toList(), HttpStatus.CREATED.value(), CATEGORY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<CategoryResponseDTO> updateCategory(UpdateCategoryRequestDTO updateCategoryRequestDTO, Long id) {

        Category category = findCategoryById(id);

        CategoryFactory.update(category, updateCategoryRequestDTO);

        categoryRepository.save(category);

        return new ResponseDTO<>(CategoryResponseDTO.from(category), HttpStatus.OK.value(), CATEGORY_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteCategory(Long id) {

        Category category = findCategoryById(id);

        categoryRepository.delete(category);
    }
}
