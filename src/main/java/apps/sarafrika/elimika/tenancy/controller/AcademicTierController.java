package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.AcademicTierDTO;
import apps.sarafrika.elimika.tenancy.services.AcademicTierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/academic-tiers")
@RequiredArgsConstructor
@Tag(name = "Academic Tiers API", description = "Read-only catalogue of schooling levels (Grade 7, Form 2, ...)")
public class AcademicTierController {

    /**
     * A reference list, not tenant data: the same sixteen Kenyan schooling levels are returned to
     * every caller, and the endpoint takes no organisation, so there is nothing to scope to and
     * nothing one tenant could learn about another. Requiring only authentication keeps every
     * signed-in role — org admins filling in the group form, instructors, students — able to label
     * a tier without a per-organisation grant. It is not left open to anonymous callers because the
     * rest of the API is authenticated and an unauthenticated route is a needless surface.
     */
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final AcademicTierService academicTierService;

    @Operation(summary = "List academic tiers",
            description = "Returns the active platform-wide schooling levels for an education system, "
                    + "ordered by tier order. Read-only reference data.")
    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<ApiResponse<List<AcademicTierDTO>>> listTiers(
            @RequestParam(name = "education_system", required = false, defaultValue = "KE") String educationSystem) {
        List<AcademicTierDTO> tiers = academicTierService.getPlatformTiers(educationSystem);
        return ResponseEntity.ok(ApiResponse.success(tiers, "Academic tiers retrieved successfully"));
    }
}
