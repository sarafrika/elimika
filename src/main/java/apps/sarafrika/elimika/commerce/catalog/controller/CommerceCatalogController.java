package apps.sarafrika.elimika.commerce.catalog.controller;

import apps.sarafrika.elimika.commerce.catalog.dto.CommerceCatalogItemDTO;
import apps.sarafrika.elimika.commerce.catalog.dto.UpsertCommerceCatalogItemRequest;
import apps.sarafrika.elimika.commerce.catalog.service.CommerceCatalogService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CommerceCatalogController.API_ROOT)
@RequiredArgsConstructor
@Tag(name = "Commerce Catalog", description = "Manage mappings between Elimika courses/classes and internal commerce variants")
public class CommerceCatalogController {

    public static final String API_ROOT = "/api/v1/commerce/catalog";

    private final CommerceCatalogService catalogService;

    @Operation(summary = "List catalog mappings", description = "Returns catalog items optionally filtered by active status")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommerceCatalogItemDTO>>> listCatalogItems(
            @RequestParam(name = "active_only", required = false) Boolean activeOnly) {
        List<CommerceCatalogItemDTO> items = catalogService.listAll(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(items, "Catalog items retrieved successfully"));
    }

    @Operation(summary = "Get catalog mapping by course")
    @GetMapping("/by-course/{courseUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogItemDTO>> getByCourse(
            @PathVariable UUID courseUuid) {
        return catalogService.getByCourse(courseUuid)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Catalog item retrieved successfully")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catalog item not found for course")));
    }

    @Operation(summary = "Get catalog mapping by class definition")
    @GetMapping("/by-class/{classUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogItemDTO>> getByClass(
            @PathVariable("classUuid") UUID classDefinitionUuid) {
        return catalogService.getByClassDefinition(classDefinitionUuid)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Catalog item retrieved successfully")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catalog item not found for class definition")));
    }

    @Operation(summary = "Update catalog mapping", description = "Updates internal variant identifiers or status for an existing mapping")
    @PutMapping("/{catalogUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogItemDTO>> updateCatalogItem(
            @PathVariable UUID catalogUuid,
            @Valid @RequestBody UpsertCommerceCatalogItemRequest request) {
        CommerceCatalogItemDTO dto = catalogService.updateItem(catalogUuid, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Catalog item updated successfully"));
    }
}
