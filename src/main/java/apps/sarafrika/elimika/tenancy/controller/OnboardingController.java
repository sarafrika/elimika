package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.OnboardingDTO;
import apps.sarafrika.elimika.tenancy.dto.OnboardingSummaryDTO;
import apps.sarafrika.elimika.tenancy.services.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Onboarding", description = "One onboarding wizard for every domain; shared steps done once count for all")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final DomainSecurityService domainSecurityService;

    @Operation(operationId = "getMyOnboarding", summary = "Onboarding state of each domain the caller holds",
            description = "Works while a domain is pending approval.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OnboardingSummaryDTO>>> myOnboarding() {
        return ResponseEntity.ok(ApiResponse.success(onboardingService.domainsOf(currentUser()),
                "Onboarding retrieved successfully"));
    }

    @Operation(operationId = "getDomainOnboarding", summary = "Ordered onboarding steps for one domain",
            description = "Steps marked shared hold user-owned data (account, professional profile, skills wallet), so "
                    + "they show as complete when another domain already filled them. A domain not yet requested "
                    + "returns a preview with requested=false.")
    @GetMapping("/{domain}")
    public ResponseEntity<ApiResponse<OnboardingDTO>> domainOnboarding(
            @Parameter(description = "student, instructor, course_creator, organisation_user or parent")
            @PathVariable String domain) {
        return ResponseEntity.ok(ApiResponse.success(onboardingService.onboarding(currentUser(), parse(domain)),
                "Onboarding retrieved successfully"));
    }

    @Operation(operationId = "submitDomainOnboarding", summary = "Submit a domain's onboarding",
            description = "Validates the required steps. Domains that need approval move to submitted and admins are "
                    + "asked to review; others are recorded as complete and stay active. 409 when steps are missing, "
                    + "or the domain is already submitted or approved; 404 when the domain was never requested.")
    @PostMapping("/{domain}/submit")
    public ResponseEntity<ApiResponse<OnboardingDTO>> submit(@PathVariable String domain) {
        return ResponseEntity.ok(ApiResponse.success(onboardingService.submit(currentUser(), parse(domain)),
                "Onboarding submitted"));
    }

    private UUID currentUser() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new ResourceNotFoundException("No user record for the authenticated caller");
        }
        return userUuid;
    }

    private static UserDomain parse(String domain) {
        try {
            return UserDomain.valueOf(domain.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
    }
}
