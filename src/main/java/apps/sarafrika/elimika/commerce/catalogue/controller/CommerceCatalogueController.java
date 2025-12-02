package apps.sarafrika.elimika.commerce.catalogue.controller;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
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
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping(CommerceCatalogueController.API_ROOT)
@RequiredArgsConstructor
@Tag(name = "Commerce Catalogue", description = "Manage mappings between Elimika courses/classes and internal commerce variants")
public class CommerceCatalogueController {

    public static final String API_ROOT = "/api/v1/commerce/catalogue";

    private final CommerceCatalogueService catalogService;

    @Operation(summary = "List catalogue mappings", description = "Returns catalogue items optionally filtered by active status")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommerceCatalogueItemDTO>>> listCatalogItems(
            @RequestParam(name = "active_only", required = false) Boolean activeOnly) {
        List<CommerceCatalogueItemDTO> items = catalogService.listAll(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(items, "Catalogue items retrieved successfully"));
    }

    @Operation(summary = "Create catalogue mapping", description = "Creates a catalogue mapping for a course or class")
    @PostMapping
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> createCatalogItem(
            @Valid @RequestBody UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItemDTO dto = catalogService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Catalogue item created successfully"));
    }

    @Operation(summary = "Get catalogue mapping by course")
    @GetMapping("/by-course/{courseUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> getByCourse(
            @PathVariable UUID courseUuid) {
        return catalogService.getByCourse(courseUuid)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Catalogue item retrieved successfully")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catalogue item not found for course")));
    }

    @Operation(summary = "Get catalogue mapping by class definition")
    @GetMapping("/by-class/{classUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> getByClass(
            @PathVariable("classUuid") UUID classDefinitionUuid) {
        return catalogService.getByClassDefinition(classDefinitionUuid)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Catalogue item retrieved successfully")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catalogue item not found for class definition")));
    }

    @Operation(summary = "Update catalogue mapping", description = "Updates internal variant identifiers or status for an existing mapping")
    @PutMapping("/{catalogUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> updateCatalogItem(
            @PathVariable UUID catalogUuid,
            @Valid @RequestBody UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItemDTO dto = catalogService.updateItem(catalogUuid, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Catalogue item updated successfully"));
    }
}
