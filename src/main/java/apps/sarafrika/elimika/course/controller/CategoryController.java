package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.course.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing course categories")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new category with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Category with the same name already exists")
    })
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> createCategory(
            @Valid @RequestBody CategoryDTO categoryDTO) {
            CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(apps.sarafrika.elimika.common.dto.ApiResponse.success(
                            createdCategory, "Category created successfully"));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get category by UUID", description = "Retrieves a specific category by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> getCategoryByUuid(
            @PathVariable UUID uuid) {
            CategoryDTO category = categoryService.getCategoryByUuid(uuid);
            return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(
                    category, "Category retrieved successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves all categories with pagination")
    @ApiResponse(responseCode = "200", description = "List of categories retrieved")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CategoryDTO>>> getAllCategories(
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Page<CategoryDTO> categories = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(
                PagedDTO.from(categories, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()), "Categories retrieved successfully"));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update a category", description = "Updates an existing category with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category with the same name already exists")
    })
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable UUID uuid,
            @Valid @RequestBody CategoryDTO categoryDTO) {
            CategoryDTO updatedCategory = categoryService.updateCategory(uuid, categoryDTO);
            return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(
                    updatedCategory, "Category updated successfully"));
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a category", description = "Deletes a category by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID uuid) {
            categoryService.deleteCategory(uuid);
            return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search categories", description = "Search categories based on multiple criteria")
    @ApiResponse(responseCode = "200", description = "List of matching categories retrieved")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CategoryDTO>>> searchCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {

        Map<String, String> searchParams = new HashMap<>();
        if (name != null) searchParams.put("name", name);
        if (description != null) searchParams.put("description", description);

        Page<CategoryDTO> categories = categoryService.searchCategories(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(
                PagedDTO.from(categories, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()), "Categories search completed successfully"));
    }
}