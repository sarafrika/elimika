package apps.sarafrika.elimika.profile.controller;

import apps.sarafrika.elimika.profile.internal.service.ProfileDocumentFiles;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSummaryDTO;
import apps.sarafrika.elimika.profile.spi.UserAchievementDTO;
import apps.sarafrika.elimika.profile.spi.UserCertificationDTO;
import apps.sarafrika.elimika.profile.spi.UserCompetencyDTO;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.profile.spi.UserEducationDTO;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.profile.spi.UserMembershipDTO;
import apps.sarafrika.elimika.profile.spi.UserPortfolioItemDTO;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Reads another user's professional profile, all of it behind {@code profileCredentialSecurityService}. */
@RestController
@RequestMapping(UserProfessionalProfileController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "User Professional Profile", description = "Read a user's professional profile and skills wallet")
@PreAuthorize("@profileCredentialSecurityService.canReadCredentials(#userUuid)")
public class UserProfessionalProfileController {

    public static final String API_ROOT_PATH = "/api/v1/users/{userUuid}/profile";

    private final ProfessionalProfileService profileService;
    private final ProfileDocumentFiles documentFiles;

    @Operation(summary = "Get a user's professional profile",
            description = "Answered to the user, a platform admin, staff of an organisation the user belongs to, "
                    + "and whoever is reviewing an application the user lodged.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileSummaryDTO>> getSummary(@PathVariable UUID userUuid) {
        return ok(profileService.getSummary(userUuid));
    }

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<UserSkillDTO>>> skills(@PathVariable UUID userUuid) {
        return ok(profileService.skills().list(userUuid));
    }

    @GetMapping("/education")
    public ResponseEntity<ApiResponse<List<UserEducationDTO>>> education(@PathVariable UUID userUuid) {
        return ok(profileService.education().list(userUuid));
    }

    @GetMapping("/experience")
    public ResponseEntity<ApiResponse<List<UserExperienceDTO>>> experience(@PathVariable UUID userUuid) {
        return ok(profileService.experience().list(userUuid));
    }

    @GetMapping("/memberships")
    public ResponseEntity<ApiResponse<List<UserMembershipDTO>>> memberships(@PathVariable UUID userUuid) {
        return ok(profileService.memberships().list(userUuid));
    }

    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<List<UserCertificationDTO>>> certifications(@PathVariable UUID userUuid) {
        return ok(profileService.certifications().list(userUuid));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<List<UserPortfolioItemDTO>>> portfolio(@PathVariable UUID userUuid) {
        return ok(profileService.portfolio().list(userUuid));
    }

    @GetMapping("/competencies")
    public ResponseEntity<ApiResponse<List<UserCompetencyDTO>>> competencies(@PathVariable UUID userUuid) {
        return ok(profileService.competencies().list(userUuid));
    }

    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<List<UserAchievementDTO>>> achievements(@PathVariable UUID userUuid) {
        return ok(profileService.achievements().list(userUuid));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<UserDocumentDTO>>> documents(@PathVariable UUID userUuid) {
        return ok(profileService.documents().list(userUuid));
    }

    @GetMapping("/documents/{documentUuid}/file")
    public ResponseEntity<Resource> documentFile(@PathVariable UUID userUuid, @PathVariable UUID documentUuid) {
        return documentFiles.serve(userUuid, documentUuid);
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T body) {
        return ResponseEntity.ok(ApiResponse.success(body, "Profile fetched successfully"));
    }
}
