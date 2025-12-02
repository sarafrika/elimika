package apps.sarafrika.elimika.commerce.catalogue.controller;

import apps.sarafrika.elimika.commerce.internal.service.CatalogueBackfillService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commerce/catalogue")
@RequiredArgsConstructor
@Tag(name = "Commerce Catalogue Admin", description = "Operational endpoints for catalogue maintenance")
public class CatalogueBackfillController {

    private final CatalogueBackfillService catalogueBackfillService;

    @Operation(summary = "Backfill catalogue", description = "Rebuilds catalogue items for all published courses/classes")
    @PostMapping("/backfill")
    public ResponseEntity<ApiResponse<Integer>> backfillCatalogue() {
        int processed = catalogueBackfillService.backfillPublishedCatalogue();
        return ResponseEntity.ok(ApiResponse.success(processed, "Catalogue backfill completed"));
    }
}
