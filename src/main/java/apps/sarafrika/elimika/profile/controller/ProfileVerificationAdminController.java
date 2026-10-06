package apps.sarafrika.elimika.profile.controller;

import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users/{userUuid}/profile")
@RequiredArgsConstructor
@Tag(name = "Professional Profile Admin", description = "Platform-admin verification of profile items")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class ProfileVerificationAdminController {

    private final ProfessionalProfileService profileService;

    @Operation(summary = "Verify a profile item",
            description = "Marks a skill, certification, competency or document VERIFIED or REJECTED. The verdict "
                    + "holds for every domain the user has; editing the claim later sends it back to PENDING.")
    @PostMapping("/{section}/{itemUuid}/verification")
    public ResponseEntity<ApiResponse<Void>> verify(
            @PathVariable UUID userUuid,
            @Parameter(schema = @Schema(allowableValues = {"skills", "certifications", "competencies", "documents"}))
            @PathVariable String section,
            @PathVariable UUID itemUuid,
            @Valid @RequestBody ProfileVerificationRequest request) {
        profileService.verify(userUuid, ProfileSection.fromPath(section), itemUuid, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Profile item verification recorded"));
    }
}
