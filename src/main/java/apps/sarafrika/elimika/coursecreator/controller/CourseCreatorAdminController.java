package apps.sarafrika.elimika.coursecreator.controller;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorModerationRequest;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorOnboardingStateDTO;
import apps.sarafrika.elimika.coursecreator.dto.WalletVerificationRequest;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorOnboardingService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorWalletService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/admin/course-creators")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course Creator Admin", description = "Administrative moderation endpoints for course creator onboarding")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class CourseCreatorAdminController {

    private final CourseCreatorOnboardingService courseCreatorOnboardingService;
    private final CourseCreatorWalletService courseCreatorWalletService;

    @Operation(summary = "Verify a skills wallet item",
            description = "Marks a skill, competency or certification VERIFIED or REJECTED after checking its evidence.")
    @PostMapping("/{uuid}/wallet/{section}/{itemUuid}/verification")
    public ResponseEntity<ApiResponse<Void>> verifyWalletItem(
            @PathVariable UUID uuid,
            @Parameter(schema = @Schema(allowableValues = {"skills", "competencies", "certifications"}))
            @PathVariable String section,
            @PathVariable UUID itemUuid,
            @Valid @RequestBody WalletVerificationRequest request) {
        courseCreatorWalletService.verifyItem(uuid, section, itemUuid, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Wallet item verification recorded"));
    }

    @Operation(
            summary = "Moderate course creator verification",
            description = "Approves, rejects or revokes a course creator onboarding submission."
    )
    @PostMapping("/{uuid}/moderate")
    public ResponseEntity<ApiResponse<CourseCreatorOnboardingStateDTO>> moderateCourseCreator(
            @Parameter(description = "Course creator UUID", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Moderation action",
                    schema = @Schema(allowableValues = {"approve", "reject", "revoke"}), required = true)
            @RequestParam("action") String action,
            @Valid @RequestBody(required = false) CourseCreatorModerationRequest request) {

        String reason = request == null ? null : request.reason();
        CourseCreatorOnboardingStateDTO state = courseCreatorOnboardingService.moderate(uuid, action, reason);
        return ResponseEntity.ok(ApiResponse.success(state, "Course creator moderation completed successfully"));
    }
}
