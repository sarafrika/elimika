package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CategoryInterestsDTO;
import apps.sarafrika.elimika.course.dto.CategoryInterestsRequest;
import apps.sarafrika.elimika.course.internal.interest.CategoryInterestService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's course category interests, one set per domain. Any signed-in account may keep them, including
 * for a domain it has not requested yet, because onboarding asks for interests first.
 */
@RestController
@RequestMapping("/api/v1/users/me/interests")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Interests", description = "Course categories the caller is interested in, per domain")
public class CategoryInterestController {

    private static final String DOMAIN = "student, instructor, course_creator or organisation_user";

    private final CategoryInterestService service;

    @Operation(operationId = "getMyInterests", summary = "My interests for one domain",
            description = "Empty when none are saved. 400 for any other domain.")
    @GetMapping("/{domain}")
    public ResponseEntity<ApiResponse<CategoryInterestsDTO>> mine(
            @Parameter(description = DOMAIN) @PathVariable String domain) {
        return ResponseEntity.ok(ApiResponse.success(service.mine(domain), "Interests retrieved successfully"));
    }

    @Operation(operationId = "updateMyInterests", summary = "Replace my interests for one domain",
            description = "Saves exactly the categories sent, in that order; an empty list clears them. "
                    + "400 for a null entry, an unknown or inactive category, or an unknown domain.")
    @PutMapping("/{domain}")
    public ResponseEntity<ApiResponse<CategoryInterestsDTO>> replaceMine(
            @Parameter(description = DOMAIN) @PathVariable String domain,
            @Valid @RequestBody CategoryInterestsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.replaceMine(domain, request.categoryUuids()),
                "Interests saved successfully"));
    }
}
