package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CategoryDTO;
import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import apps.sarafrika.elimika.course.dto.DifficultyLevelDTO;
import apps.sarafrika.elimika.course.dto.GradingLevelDTO;
import apps.sarafrika.elimika.course.service.CategoryService;
import apps.sarafrika.elimika.course.service.ContentTypeService;
import apps.sarafrika.elimika.course.service.DifficultyLevelService;
import apps.sarafrika.elimika.course.service.GradingLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for system configuration and supporting entities.
 */
@RestController
@RequestMapping(SystemConfigController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "System Configuration", description = "Management of categories, content types, difficulty levels, and other system configurations")
public class SystemConfigController {

    public static final String API_ROOT_PATH = "/api/v1/config";

    private final CategoryService categoryService;
    private final ContentTypeService contentTypeService;
    private final DifficultyLevelService difficultyLevelService;
    private final GradingLevelService gradingLevelService;

    // ===== CATEGORIES =====

    @Operation(
            summary = "Create category",
            description = "Creates a new category for organizing courses and programs."
    )
    @PostMapping("/categories")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> createCategory(
            @Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdCategory, "Category created successfully"));
    }

    @Operation(
            summary = "Get all categories",
            description = "Retrieves paginated list of all categories."
    )
    @GetMapping("/categories")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CategoryDTO>>> getAllCategories(
            Pageable pageable) {
        Page<CategoryDTO> categories = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(categories, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Categories retrieved successfully"));
    }

    @Operation(
            summary = "Get root categories",
            description = "Retrieves all top-level categories (no parent)."
    )
    @GetMapping("/categories/root")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<CategoryDTO>>> getRootCategories() {
        List<CategoryDTO> rootCategories = categoryService.getRootCategories();
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(rootCategories, "Root categories retrieved successfully"));
    }

    @Operation(
            summary = "Get subcategories",
            description = "Retrieves all subcategories for a specific parent category."
    )
    @GetMapping("/categories/{parentUuid}/subcategories")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<CategoryDTO>>> getSubCategories(
            @PathVariable UUID parentUuid) {
        List<CategoryDTO> subCategories = categoryService.getSubCategories(parentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(subCategories, "Subcategories retrieved successfully"));
    }

    @Operation(
            summary = "Get category by UUID",
            description = "Retrieves a specific category by its UUID."
    )
    @GetMapping("/categories/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> getCategoryByUuid(
            @PathVariable UUID uuid) {
        CategoryDTO category = categoryService.getCategoryByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(category, "Category retrieved successfully"));
    }

    @Operation(
            summary = "Update category",
            description = "Updates an existing category."
    )
    @PutMapping("/categories/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable UUID uuid,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO updatedCategory = categoryService.updateCategory(uuid, categoryDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedCategory, "Category updated successfully"));
    }

    @Operation(
            summary = "Delete category",
            description = "Removes a category if it has no subcategories or associated courses."
    )
    @DeleteMapping("/categories/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> deleteCategory(@PathVariable UUID uuid) {
        categoryService.deleteCategory(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Category deleted successfully", "Category has been removed"));
    }

    // ===== CONTENT TYPES =====

    @Operation(
            summary = "Create content type",
            description = "Creates a new content type for lesson content classification."
    )
    @PostMapping("/content-types")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<ContentTypeDTO>> createContentType(
            @Valid @RequestBody ContentTypeDTO contentTypeDTO) {
        ContentTypeDTO createdContentType = contentTypeService.createContentType(contentTypeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdContentType, "Content type created successfully"));
    }

    @Operation(
            summary = "Get all content types",
            description = "Retrieves paginated list of all content types."
    )
    @GetMapping("/content-types")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<ContentTypeDTO>>> getAllContentTypes(
            Pageable pageable) {
        Page<ContentTypeDTO> contentTypes = contentTypeService.getAllContentTypes(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(contentTypes, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Content types retrieved successfully"));
    }

    @Operation(
            summary = "Get media content types",
            description = "Retrieves content types for media files (video, audio, images)."
    )
    @GetMapping("/content-types/media")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<ContentTypeDTO>>> getMediaContentTypes() {
        List<ContentTypeDTO> mediaTypes = contentTypeService.getMediaContentTypes();
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(mediaTypes, "Media content types retrieved successfully"));
    }

    @Operation(
            summary = "Check MIME type support",
            description = "Checks if a specific MIME type is supported by the system."
    )
    @GetMapping("/content-types/mime-support/{mimeType}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<Boolean>> checkMimeTypeSupport(
            @PathVariable String mimeType) {
        boolean isSupported = contentTypeService.isMimeTypeSupported(mimeType);
        String message = isSupported ? "MIME type is supported" : "MIME type is not supported";
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(isSupported, message));
    }

    @Operation(
            summary = "Update content type",
            description = "Updates an existing content type."
    )
    @PutMapping("/content-types/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<ContentTypeDTO>> updateContentType(
            @PathVariable UUID uuid,
            @Valid @RequestBody ContentTypeDTO contentTypeDTO) {
        ContentTypeDTO updatedContentType = contentTypeService.updateContentType(uuid, contentTypeDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedContentType, "Content type updated successfully"));
    }

    @Operation(
            summary = "Delete content type",
            description = "Removes a content type if no lesson content is using it."
    )
    @DeleteMapping("/content-types/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> deleteContentType(@PathVariable UUID uuid) {
        if (!contentTypeService.canDeleteContentType(uuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.common.dto.ApiResponse
                            .error("Cannot delete content type that is being used by lesson content"));
        }
        contentTypeService.deleteContentType(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Content type deleted successfully", "Content type has been removed"));
    }

    // ===== DIFFICULTY LEVELS =====

    @Operation(
            summary = "Create difficulty level",
            description = "Creates a new difficulty level for course classification."
    )
    @PostMapping("/difficulty-levels")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<DifficultyLevelDTO>> createDifficultyLevel(
            @Valid @RequestBody DifficultyLevelDTO difficultyLevelDTO) {
        DifficultyLevelDTO createdLevel = difficultyLevelService.createDifficultyLevel(difficultyLevelDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdLevel, "Difficulty level created successfully"));
    }

    @Operation(
            summary = "Get all difficulty levels",
            description = "Retrieves all difficulty levels in order."
    )
    @GetMapping("/difficulty-levels")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<DifficultyLevelDTO>>> getAllDifficultyLevels() {
        List<DifficultyLevelDTO> levels = difficultyLevelService.getAllDifficultyLevelsInOrder();
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(levels, "Difficulty levels retrieved successfully"));
    }

    @Operation(
            summary = "Reorder difficulty levels",
            description = "Updates the order of difficulty levels."
    )
    @PostMapping("/difficulty-levels/reorder")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> reorderDifficultyLevels(
            @RequestBody List<UUID> levelUuids) {
        difficultyLevelService.reorderDifficultyLevels(levelUuids);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Difficulty levels reordered successfully", "Level order updated"));
    }

    @Operation(
            summary = "Update difficulty level",
            description = "Updates an existing difficulty level."
    )
    @PutMapping("/difficulty-levels/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<DifficultyLevelDTO>> updateDifficultyLevel(
            @PathVariable UUID uuid,
            @Valid @RequestBody DifficultyLevelDTO difficultyLevelDTO) {
        DifficultyLevelDTO updatedLevel = difficultyLevelService.updateDifficultyLevel(uuid, difficultyLevelDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedLevel, "Difficulty level updated successfully"));
    }

    @Operation(
            summary = "Delete difficulty level",
            description = "Removes a difficulty level if no courses are using it."
    )
    @DeleteMapping("/difficulty-levels/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> deleteDifficultyLevel(@PathVariable UUID uuid) {
        if (!difficultyLevelService.canDeleteLevel(uuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.common.dto.ApiResponse
                            .error("Cannot delete difficulty level that is being used by courses"));
        }
        difficultyLevelService.deleteDifficultyLevel(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Difficulty level deleted successfully", "Level has been removed"));
    }

    // ===== GRADING LEVELS =====

    @Operation(
            summary = "Create grading level",
            description = "Creates a new grading level for assessment scoring."
    )
    @PostMapping("/grading-levels")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<GradingLevelDTO>> createGradingLevel(
            @Valid @RequestBody GradingLevelDTO gradingLevelDTO) {
        GradingLevelDTO createdLevel = gradingLevelService.createGradingLevel(gradingLevelDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdLevel, "Grading level created successfully"));
    }

    @Operation(
            summary = "Get all grading levels",
            description = "Retrieves paginated list of all grading levels."
    )
    @GetMapping("/grading-levels")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<GradingLevelDTO>>> getAllGradingLevels(
            Pageable pageable) {
        Page<GradingLevelDTO> levels = gradingLevelService.getAllGradingLevels(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(levels, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Grading levels retrieved successfully"));
    }

    @Operation(
            summary = "Update grading level",
            description = "Updates an existing grading level."
    )
    @PutMapping("/grading-levels/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<GradingLevelDTO>> updateGradingLevel(
            @PathVariable UUID uuid,
            @Valid @RequestBody GradingLevelDTO gradingLevelDTO) {
        GradingLevelDTO updatedLevel = gradingLevelService.updateGradingLevel(uuid, gradingLevelDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedLevel, "Grading level updated successfully"));
    }

    @Operation(
            summary = "Delete grading level",
            description = "Removes a grading level."
    )
    @DeleteMapping("/grading-levels/{uuid}")
    public ResponseEntity<Void> deleteGradingLevel(@PathVariable UUID uuid) {
        gradingLevelService.deleteGradingLevel(uuid);
        return ResponseEntity.noContent().build();
    }

    // ===== SEARCH ENDPOINTS =====

    @Operation(
            summary = "Search categories",
            description = """
                    Search categories with filtering options.
                    
                    **Common Category Search Examples:**
                    - `name_like=technology` - Categories with "technology" in name
                    - `parentUuid=null` - Root categories only
                    - `parentUuid=uuid` - Subcategories of specific parent
                    - `isActive=true` - Only active categories
                    """
    )
    @GetMapping("/categories/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<CategoryDTO>>> searchCategories(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CategoryDTO> categories = categoryService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(categories, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Category search completed successfully"));
    }

    @Operation(
            summary = "Search content types",
            description = """
                    Search content types with filtering options.
                    
                    **Common Content Type Search Examples:**
                    - `name_like=video` - Content types with "video" in name
                    - `mimeTypes_like=image/` - Image content types
                    - `maxFileSizeMb_gte=100` - Large file content types
                    """
    )
    @GetMapping("/content-types/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<ContentTypeDTO>>> searchContentTypes(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<ContentTypeDTO> contentTypes = contentTypeService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(contentTypes, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Content type search completed successfully"));
    }
}