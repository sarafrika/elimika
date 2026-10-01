package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CatalogueSearchResponse;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Level;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Price;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Query;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Show;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchService.Sort;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The public catalogue page: one search across courses and programmes, ranked together. Open to
 * anonymous callers; every caller sees the public catalogue only.
 */
@RestController
@RequestMapping("/api/v1/catalogue")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = "Public catalogue search across courses and programmes")
public class CatalogueSearchController {

    private final CatalogueSearchService catalogueSearchService;

    @GetMapping("/search")
    @Operation(operationId = "searchCatalogue", summary = "Search the public catalogue",
            description = "One ranked list of public courses and programmes (is_public courses; published, active, "
                    + "admin-approved programmes) for every caller, signed in or not. With q the two types are merged "
                    + "by relevance (typo-tolerant); without q it is a browse. Filters: show, category_uuid, level, "
                    + "price, creator_uuid. Facet counts reflect every other active filter with each group's own "
                    + "selection left out, so they stay useful while filtering; show narrows the category, level and "
                    + "price counts to the shown types. A programme matches a level when any of its member courses has "
                    + "it. Counts (lessons, learners, classes) are live from the database; hits that are no longer "
                    + "public are dropped and the total restated. 503 \"Search is unavailable\" when search is "
                    + "disabled or unavailable; there is no database fallback.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "A page of catalogue items with facets")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "An unknown show, level, price or sort value, a malformed UUID, or page/size out of range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Search is disabled or unavailable")
    public ResponseEntity<ApiResponse<CatalogueSearchResponse>> searchCatalogue(
            @Parameter(description = "Free-text query; empty or absent browses the catalogue")
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Which types to list (default all)",
                    schema = @Schema(allowableValues = {"all", "courses", "programmes"}, defaultValue = "all"))
            @RequestParam(value = "show", required = false) String show,
            @Parameter(description = "Category UUIDs; repeat the parameter or pass a comma-separated list. "
                    + "A result matches when it is in any of them.",
                    array = @ArraySchema(schema = @Schema(type = "string", format = "uuid")))
            @RequestParam(value = "category_uuid", required = false) List<String> categoryUuids,
            @Parameter(description = "Levels; repeatable or comma-separated. A course matches its difficulty; a "
                    + "programme matches when any member course has the level.",
                    array = @ArraySchema(schema = @Schema(type = "string", allowableValues = {"beginner", "intermediate", "advanced"})))
            @RequestParam(value = "level", required = false) List<String> levels,
            @Parameter(description = "free or paid; both (or none) means no price filter",
                    array = @ArraySchema(schema = @Schema(type = "string", allowableValues = {"free", "paid"})))
            @RequestParam(value = "price", required = false) List<String> prices,
            @Parameter(description = "Only results by this course creator",
                    schema = @Schema(type = "string", format = "uuid"))
            @RequestParam(value = "creator_uuid", required = false) String creatorUuid,
            @Parameter(description = "Ordering (default relevance with q, popular without). newest: created first; "
                    + "rating: Bayesian-smoothed review rating; popular: enrolments in the last 30 days",
                    schema = @Schema(allowableValues = {"relevance", "newest", "rating", "popular"}))
            @RequestParam(value = "sort", required = false) String sort,
            @Parameter(description = "0-based page number", schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-48", schema = @Schema(defaultValue = "24", minimum = "1", maximum = "48"))
            @RequestParam(value = "size", required = false, defaultValue = "24") int size) {
        Query query = new Query(
                q,
                CatalogueSearchService.parseEnum("show", show, Show.class),
                CatalogueSearchService.parseUuids("category_uuid", categoryUuids),
                CatalogueSearchService.parseEnums("level", levels, Level.class),
                CatalogueSearchService.parseEnums("price", prices, Price.class),
                CatalogueSearchService.parseUuidParam("creator_uuid", creatorUuid),
                CatalogueSearchService.parseEnum("sort", sort, Sort.class),
                page,
                size);
        return ResponseEntity.ok(ApiResponse.success(catalogueSearchService.search(query),
                "Catalogue retrieved successfully"));
    }
}
