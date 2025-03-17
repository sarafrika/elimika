package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.request.UpdateCategoryRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.course.service.CategoryService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = CategoryController.ROOT_PATH)
class CategoryController {

    protected static final String ROOT_PATH = "api/v1/categories";

    private final CategoryService categoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<CategoryResponseDTO> getAllCategories(CategoryRequestDTO categoryRequestDTO, Pageable pageable) {

        return categoryService.findAllCategories(categoryRequestDTO, pageable);
    }

    @GetMapping(path = "{uuid}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<CategoryResponseDTO> getCategory(final @RequestParam("uuid") Long categoryId) {

        return categoryService.findCategory(categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<CategoryResponseDTO> createCategory(@RequestBody CreateCategoryRequestDTO createCategoryRequestDTO) {

        return categoryService.createCategory(createCategoryRequestDTO);
    }

    @PutMapping(path = "{uuid}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<CategoryResponseDTO> updateCategory(@RequestBody UpdateCategoryRequestDTO updateCategoryRequestDTO, @PathVariable Long categoryId) {

        return categoryService.updateCategory(updateCategoryRequestDTO, categoryId);
    }

    @DeleteMapping(path = "{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCategory(@PathVariable Long categoryId) {

        categoryService.deleteCategory(categoryId);
    }
}
