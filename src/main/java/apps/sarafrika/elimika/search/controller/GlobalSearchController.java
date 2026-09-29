package apps.sarafrika.elimika.search.controller;

import apps.sarafrika.elimika.search.dto.GlobalSearchResponse;
import apps.sarafrika.elimika.search.dto.TypeSearchResponse;
import apps.sarafrika.elimika.search.internal.global.GlobalSearchService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global search across every index. Open to anonymous callers: each type's provider decides what the
 * caller may see, and an anonymous caller gets the public boundary of courses, programs,
 * organisations and classes only.
 * <p>
 * Always registered, so that with search off the routes answer 503 rather than 404.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Global search across courses, programs, classes, jobs, instructors, organisations, people and rubrics")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @Operation(summary = "Global search",
            description = "Searches every type the caller may see (or those named in types) and returns up to limit hits "
                    + "per type, grouped by type in the order requested, plus the total per type. Types: courses, "
                    + "programs, classes, marketplace_jobs, instructors, organisations, people, rubrics. A type the "
                    + "caller may not see, or whose index is not read-enabled, is skipped silently; an unknown type "
                    + "is a 400. Anonymous callers see public courses, programs, organisations and classes. People "
                    + "are visible to platform admins, and to organisation managers by name within their "
                    + "organisations. Results come from the index without a database round trip, so a change can "
                    + "take a few seconds to show. 503 when search is disabled or unavailable.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hits grouped by type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "q shorter than 2 characters, limit outside 1-20, or an unknown type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Search is disabled or unavailable")
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> search(
            @Parameter(description = "Query text, at least 2 characters", required = true)
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Comma-separated types, e.g. courses,programs; every type when omitted")
            @RequestParam(value = "types", required = false) String types,
            @Parameter(description = "Hits per type, 1-20 (default 5)")
            @RequestParam(value = "limit", required = false) Integer limit) {
        GlobalSearchResponse response = globalSearchService.search(q, types, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Search results retrieved successfully"));
    }

    @GetMapping("/{type}")
    @Operation(summary = "Search one type",
            description = "One page of one type, for a \"see all results\" view. q is optional (at least 2 characters "
                    + "when present). Other parameters filter in the field_op vocabulary (op one of eq, noteq, in, "
                    + "notin, gt, gte, lt, lte, between) over the type's filterable attributes; facets names "
                    + "filterable attributes to count values of; sort is field[,asc|desc] over sortable attributes. "
                    + "Anything outside those allow-lists is a 400. 403 when the caller may not see the type; 503 "
                    + "when search or the type is not enabled.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "A page of hits with facets")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unknown type, filter, facet or sort")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "The caller may not search this type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Search is disabled or unavailable")
    public ResponseEntity<ApiResponse<TypeSearchResponse>> searchType(
            @PathVariable String type,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "facets", required = false) String facets,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(hidden = true) @RequestParam Map<String, String> params) {
        TypeSearchResponse response = globalSearchService.searchType(type, q, params, facets, sort, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Search results retrieved successfully"));
    }

    @ExceptionHandler(SearchUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> searchUnavailable(SearchUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Search is unavailable", "Search is disabled or temporarily unavailable; try again later"));
    }
}
