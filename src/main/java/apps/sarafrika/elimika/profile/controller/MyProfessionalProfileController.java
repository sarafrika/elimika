package apps.sarafrika.elimika.profile.controller;

import apps.sarafrika.elimika.profile.internal.service.ProfileDocumentFiles;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
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
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The caller's own professional profile, one copy shared by every domain they hold. */
@RestController
@RequestMapping(MyProfessionalProfileController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "My Professional Profile", description = "The caller's own professional profile and skills wallet, shared by every domain")
public class MyProfessionalProfileController {

    public static final String API_ROOT_PATH = "/api/v1/me/profile";

    private final ProfessionalProfileService profileService;
    private final ProfileDocumentFiles documentFiles;
    private final DomainSecurityService domainSecurityService;

    @Operation(summary = "Get my professional profile", description = "Basics, item count per section and completeness.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileSummaryDTO>> getSummary() {
        return ok(profileService.getSummary(me()), "Profile fetched successfully");
    }

    @Operation(summary = "Update my profile basics",
            description = "Replaces bio, headline, website and location. Every domain profile shows the new values.")
    @PutMapping
    public ResponseEntity<ApiResponse<ProfessionalProfileDTO>> updateBasics(@Valid @RequestBody ProfessionalProfileDTO basics) {
        return ok(profileService.saveBasics(me(), basics), "Profile updated successfully");
    }

    // ===== SKILLS =====

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<UserSkillDTO>>> listSkills() {
        return list(profileService.skills());
    }

    @Operation(summary = "Add a skill", description = "Adding a skill already listed (any spelling or spacing) updates it instead.")
    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<UserSkillDTO>> addSkill(@Valid @RequestBody UserSkillDTO body) {
        return created(profileService.skills(), body);
    }

    @Operation(summary = "Update a skill", description = "Changing the name or evidence sends the skill back to PENDING verification.")
    @PutMapping("/skills/{itemUuid}")
    public ResponseEntity<ApiResponse<UserSkillDTO>> updateSkill(@PathVariable UUID itemUuid, @Valid @RequestBody UserSkillDTO body) {
        return updated(profileService.skills(), itemUuid, body);
    }

    @DeleteMapping("/skills/{itemUuid}")
    public ResponseEntity<Void> deleteSkill(@PathVariable UUID itemUuid) {
        return deleted(profileService.skills(), itemUuid);
    }

    // ===== EDUCATION =====

    @GetMapping("/education")
    public ResponseEntity<ApiResponse<List<UserEducationDTO>>> listEducation() {
        return list(profileService.education());
    }

    @PostMapping("/education")
    public ResponseEntity<ApiResponse<UserEducationDTO>> addEducation(@Valid @RequestBody UserEducationDTO body) {
        return created(profileService.education(), body);
    }

    @PutMapping("/education/{itemUuid}")
    public ResponseEntity<ApiResponse<UserEducationDTO>> updateEducation(@PathVariable UUID itemUuid,
                                                                          @Valid @RequestBody UserEducationDTO body) {
        return updated(profileService.education(), itemUuid, body);
    }

    @DeleteMapping("/education/{itemUuid}")
    public ResponseEntity<Void> deleteEducation(@PathVariable UUID itemUuid) {
        return deleted(profileService.education(), itemUuid);
    }

    // ===== EXPERIENCE =====

    @GetMapping("/experience")
    public ResponseEntity<ApiResponse<List<UserExperienceDTO>>> listExperience() {
        return list(profileService.experience());
    }

    @PostMapping("/experience")
    public ResponseEntity<ApiResponse<UserExperienceDTO>> addExperience(@Valid @RequestBody UserExperienceDTO body) {
        return created(profileService.experience(), body);
    }

    @PutMapping("/experience/{itemUuid}")
    public ResponseEntity<ApiResponse<UserExperienceDTO>> updateExperience(@PathVariable UUID itemUuid,
                                                                            @Valid @RequestBody UserExperienceDTO body) {
        return updated(profileService.experience(), itemUuid, body);
    }

    @DeleteMapping("/experience/{itemUuid}")
    public ResponseEntity<Void> deleteExperience(@PathVariable UUID itemUuid) {
        return deleted(profileService.experience(), itemUuid);
    }

    // ===== MEMBERSHIPS =====

    @GetMapping("/memberships")
    public ResponseEntity<ApiResponse<List<UserMembershipDTO>>> listMemberships() {
        return list(profileService.memberships());
    }

    @PostMapping("/memberships")
    public ResponseEntity<ApiResponse<UserMembershipDTO>> addMembership(@Valid @RequestBody UserMembershipDTO body) {
        return created(profileService.memberships(), body);
    }

    @PutMapping("/memberships/{itemUuid}")
    public ResponseEntity<ApiResponse<UserMembershipDTO>> updateMembership(@PathVariable UUID itemUuid,
                                                                            @Valid @RequestBody UserMembershipDTO body) {
        return updated(profileService.memberships(), itemUuid, body);
    }

    @DeleteMapping("/memberships/{itemUuid}")
    public ResponseEntity<Void> deleteMembership(@PathVariable UUID itemUuid) {
        return deleted(profileService.memberships(), itemUuid);
    }

    // ===== CERTIFICATIONS =====

    @GetMapping("/certifications")
    public ResponseEntity<ApiResponse<List<UserCertificationDTO>>> listCertifications() {
        return list(profileService.certifications());
    }

    @PostMapping("/certifications")
    public ResponseEntity<ApiResponse<UserCertificationDTO>> addCertification(@Valid @RequestBody UserCertificationDTO body) {
        return created(profileService.certifications(), body);
    }

    @PutMapping("/certifications/{itemUuid}")
    public ResponseEntity<ApiResponse<UserCertificationDTO>> updateCertification(
            @PathVariable UUID itemUuid, @Valid @RequestBody UserCertificationDTO body) {
        return updated(profileService.certifications(), itemUuid, body);
    }

    @DeleteMapping("/certifications/{itemUuid}")
    public ResponseEntity<Void> deleteCertification(@PathVariable UUID itemUuid) {
        return deleted(profileService.certifications(), itemUuid);
    }

    // ===== PORTFOLIO =====

    @GetMapping("/portfolio")
    public ResponseEntity<ApiResponse<List<UserPortfolioItemDTO>>> listPortfolio() {
        return list(profileService.portfolio());
    }

    @PostMapping("/portfolio")
    public ResponseEntity<ApiResponse<UserPortfolioItemDTO>> addPortfolioItem(@Valid @RequestBody UserPortfolioItemDTO body) {
        return created(profileService.portfolio(), body);
    }

    @PutMapping("/portfolio/{itemUuid}")
    public ResponseEntity<ApiResponse<UserPortfolioItemDTO>> updatePortfolioItem(
            @PathVariable UUID itemUuid, @Valid @RequestBody UserPortfolioItemDTO body) {
        return updated(profileService.portfolio(), itemUuid, body);
    }

    @DeleteMapping("/portfolio/{itemUuid}")
    public ResponseEntity<Void> deletePortfolioItem(@PathVariable UUID itemUuid) {
        return deleted(profileService.portfolio(), itemUuid);
    }

    // ===== COMPETENCIES =====

    @GetMapping("/competencies")
    public ResponseEntity<ApiResponse<List<UserCompetencyDTO>>> listCompetencies() {
        return list(profileService.competencies());
    }

    @PostMapping("/competencies")
    public ResponseEntity<ApiResponse<UserCompetencyDTO>> addCompetency(@Valid @RequestBody UserCompetencyDTO body) {
        return created(profileService.competencies(), body);
    }

    @PutMapping("/competencies/{itemUuid}")
    public ResponseEntity<ApiResponse<UserCompetencyDTO>> updateCompetency(@PathVariable UUID itemUuid,
                                                                            @Valid @RequestBody UserCompetencyDTO body) {
        return updated(profileService.competencies(), itemUuid, body);
    }

    @DeleteMapping("/competencies/{itemUuid}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID itemUuid) {
        return deleted(profileService.competencies(), itemUuid);
    }

    // ===== ACHIEVEMENTS =====

    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<List<UserAchievementDTO>>> listAchievements() {
        return list(profileService.achievements());
    }

    @PostMapping("/achievements")
    public ResponseEntity<ApiResponse<UserAchievementDTO>> addAchievement(@Valid @RequestBody UserAchievementDTO body) {
        return created(profileService.achievements(), body);
    }

    @PutMapping("/achievements/{itemUuid}")
    public ResponseEntity<ApiResponse<UserAchievementDTO>> updateAchievement(
            @PathVariable UUID itemUuid, @Valid @RequestBody UserAchievementDTO body) {
        return updated(profileService.achievements(), itemUuid, body);
    }

    @DeleteMapping("/achievements/{itemUuid}")
    public ResponseEntity<Void> deleteAchievement(@PathVariable UUID itemUuid) {
        return deleted(profileService.achievements(), itemUuid);
    }

    // ===== DOCUMENTS =====

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<UserDocumentDTO>>> listDocuments() {
        return list(profileService.documents());
    }

    @Operation(summary = "Upload a credential document",
            description = "Stores the file under the user and files it on the profile, optionally backing an education, experience or membership record.")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserDocumentDTO>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("document_type_uuid") UUID documentTypeUuid,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "education_uuid", required = false) UUID educationUuid,
            @RequestParam(value = "experience_uuid", required = false) UUID experienceUuid,
            @RequestParam(value = "membership_uuid", required = false) UUID membershipUuid,
            @RequestParam(value = "expiry_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        UserDocumentDTO document = documentFiles.upload(me(), file, documentTypeUuid, title, description,
                educationUuid, experienceUuid, membershipUuid, expiryDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @Operation(summary = "Update document details", description = "Changes the type, title, description, expiry and linked record; the file stays.")
    @PutMapping("/documents/{itemUuid}")
    public ResponseEntity<ApiResponse<UserDocumentDTO>> updateDocument(@PathVariable UUID itemUuid,
                                                                        @Valid @RequestBody UserDocumentDTO body) {
        return updated(profileService.documents(), itemUuid, body);
    }

    @DeleteMapping("/documents/{itemUuid}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID itemUuid) {
        return deleted(profileService.documents(), itemUuid);
    }

    @GetMapping("/documents/{itemUuid}/file")
    public ResponseEntity<Resource> getDocumentFile(@PathVariable UUID itemUuid) {
        return documentFiles.serve(me(), itemUuid);
    }

    private UUID me() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new AccessDeniedException("A signed-in user is required");
        }
        return userUuid;
    }

    private <D> ResponseEntity<ApiResponse<List<D>>> list(ProfileSectionService<D> section) {
        return ok(section.list(me()), "Items fetched successfully");
    }

    private <D> ResponseEntity<ApiResponse<D>> created(ProfileSectionService<D> section, D body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(section.create(me(), body), "Item saved successfully"));
    }

    private <D> ResponseEntity<ApiResponse<D>> updated(ProfileSectionService<D> section, UUID itemUuid, D body) {
        return ok(section.update(me(), itemUuid, body), "Item updated successfully");
    }

    private <D> ResponseEntity<Void> deleted(ProfileSectionService<D> section, UUID itemUuid) {
        section.delete(me(), itemUuid);
        return ResponseEntity.noContent().build();
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T body, String message) {
        return ResponseEntity.ok(ApiResponse.success(body, message));
    }
}
