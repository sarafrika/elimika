package apps.sarafrika.elimika.commerce.catalogue.controller;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @Operation(summary = "Update catalogue mapping", description = "Updates internal variant identifiers or status for an existing mapping")
    @PutMapping("/{catalogUuid}")
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> updateCatalogItem(
            @PathVariable UUID catalogUuid,
            @Valid @RequestBody UpsertCommerceCatalogueItemRequest request) {
        CommerceCatalogueItemDTO dto = catalogService.updateItem(catalogUuid, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Catalogue item updated successfully"));
    }

    @Operation(
            summary = "Search catalogue with flexible filters",
            description = """
                    Provides the standard search interface used by other modules.
                    
                    **Examples:**
                    - `publiclyVisible=true&active=true` — public, active catalogue entries
                    - `courseUuid=<uuid>` — catalogue entries for a course
                    - `classDefinitionUuid=<uuid>&active=true` — active class-level entries
                    - `variantCode_like=starter` — variant codes containing `starter`
                    
                    Supports all comparison operators accepted by the platform-wide search builder.
                    """
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedDTO<CommerceCatalogueItemDTO>>> searchCatalogue(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CommerceCatalogueItemDTO> items = catalogService.search(searchParams, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(items, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Catalogue search completed successfully"));
    }

    @Operation(summary = "Resolve catalogue mapping by course or class", description = "Tries course first, then class")
    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<CommerceCatalogueItemDTO>> resolveByCourseOrClass(
            @RequestParam(name = "course_uuid", required = false) UUID courseUuid,
            @RequestParam(name = "class_uuid", required = false) UUID classDefinitionUuid) {
        return catalogService.getByCourseOrClass(courseUuid, classDefinitionUuid)
                .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Catalogue item retrieved successfully")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Catalogue item not found")));
    }
}
