package apps.sarafrika.elimika.coursecreator.controller;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCertificationDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorExperienceDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorProfessionalMembershipDTO;
import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorCertificationService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorDocumentService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorEducationService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorExperienceService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorProfessionalMembershipService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorService;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorSkillService;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.utils.validation.PdfFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing course creator operations.
 * Provides endpoints for CRUD operations, search, and verification management.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-09-30
 */
@RestController
@RequestMapping(CourseCreatorController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Creator Management", description = "Comprehensive endpoints for managing course creators and their verification status")
public class CourseCreatorController {

    public static final String API_ROOT_PATH = "/api/v1/course-creators";

    private final CourseCreatorService courseCreatorService;
    private final CourseCreatorSkillService courseCreatorSkillService;
    private final CourseCreatorEducationService courseCreatorEducationService;
    private final CourseCreatorExperienceService courseCreatorExperienceService;
    private final CourseCreatorProfessionalMembershipService courseCreatorProfessionalMembershipService;
    private final CourseCreatorCertificationService courseCreatorCertificationService;
    private final CourseCreatorDocumentService courseCreatorDocumentService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    // ===== COURSE CREATOR BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new course creator",
            description = "Saves a new course creator profile in the system. The course creator will be unverified by default and require admin verification.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Course creator created successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> createCourseCreator(
            @Valid @RequestBody CourseCreatorDTO courseCreatorDTO) {
        CourseCreatorDTO createdCourseCreator = courseCreatorService.createCourseCreator(courseCreatorDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdCourseCreator, "Course creator created successfully"));
    }

    @Operation(
            summary = "Get course creator by UUID",
            description = "Fetches a course creator profile by their unique identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator found",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> getCourseCreatorByUuid(
            @PathVariable UUID uuid) {
        CourseCreatorDTO courseCreatorDTO = courseCreatorService.getCourseCreatorByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courseCreatorDTO, "Course creator profile fetched successfully"));
    }

    @Operation(
            summary = "Get all course creators",
            description = "Fetches a paginated list of all course creator profiles in the system."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getAllCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> courseCreators = courseCreatorService.getAllCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(courseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course creators fetched successfully"));
    }

    @Operation(
            summary = "Update a course creator",
            description = "Updates an existing course creator profile. Only allows updating mutable fields like bio, professional headline, and website.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator updated successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> updateCourseCreator(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseCreatorDTO courseCreatorDTO) {
        CourseCreatorDTO updatedCourseCreator = courseCreatorService.updateCourseCreator(uuid, courseCreatorDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCourseCreator, "Course creator updated successfully"));
    }

    @Operation(
            summary = "Delete a course creator",
            description = "Removes a course creator profile from the system. This will cascade delete associated data.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Course creator deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteCourseCreator(@PathVariable UUID uuid) {
        courseCreatorService.deleteCourseCreator(uuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search course creators",
            description = """
                    Search for course creators using flexible criteria with advanced operators.
                   \s
                    **Basic Search:**
                    - `field=value` - Exact match (default operation)
                    - `fullName=John` - Find course creators with fullName exactly "John"
                   \s
                    **Comparison Operators:**
                    - `field_gt=value` - Greater than
                    - `field_lt=value` - Less than \s
                    - `field_gte=value` - Greater than or equal
                    - `field_lte=value` - Less than or equal
                    - `createdDate_gte=2024-01-01T00:00:00` - Created after Jan 1, 2024
                   \s
                    **String Operations:**
                    - `field_like=value` - Contains (case-insensitive)
                    - `field_startswith=value` - Starts with (case-insensitive) \s
                    - `field_endswith=value` - Ends with (case-insensitive)
                    - `fullName_like=alice` - Full name contains "alice"
                   \s
                    **Boolean Operations:**
                    - `adminVerified=true` - Only verified course creators
                    - `adminVerified=false` - Only unverified course creators
                   \s
                    **List Operations:**
                    - `field_in=val1,val2,val3` - Field is in list
                    - `field_notin=val1,val2` - Field is not in list
                   \s
                    **Negation:**
                    - `field_noteq=value` - Not equal to value
                   \s
                    **Examples:**
                    - `/search?fullName_like=john&adminVerified=true`
                    - `/search?createdDate_gte=2024-01-01T00:00:00`
                    - `/search?professionalHeadline_like=content`
                   \s""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            }
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> searchCourseCreators(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorDTO> searchResults = courseCreatorService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(searchResults, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Search completed successfully"));
    }

    // ===== VERIFICATION MANAGEMENT =====

    @Operation(
            summary = "Verify a course creator",
            description = "Marks a course creator as verified by an administrator. Only system admins can perform this operation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator verified successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    @PostMapping("/{uuid}/verify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> verifyCourseCreator(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String reason) {
        CourseCreatorDTO verifiedCourseCreator = courseCreatorService.verifyCourseCreator(uuid, reason);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(verifiedCourseCreator, "Course creator verified successfully"));
    }

    @Operation(
            summary = "Unverify a course creator",
            description = "Removes verification status from a course creator. Only system admins can perform this operation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Course creator unverified successfully",
                            content = @Content(schema = @Schema(implementation = CourseCreatorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Course creator not found"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            }
    )
    @PostMapping("/{uuid}/unverify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDTO>> unverifyCourseCreator(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String reason) {
        CourseCreatorDTO unverifiedCourseCreator = courseCreatorService.unverifyCourseCreator(uuid, reason);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(unverifiedCourseCreator, "Course creator verification removed successfully"));
    }

    @Operation(
            summary = "Check if course creator is verified",
            description = "Returns the verification status of a course creator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verification status retrieved"),
                    @ApiResponse(responseCode = "404", description = "Course creator not found")
            }
    )
    @GetMapping("/{uuid}/verification-status")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Boolean>> isCourseCreatorVerified(
            @PathVariable UUID uuid) {
        boolean isVerified = courseCreatorService.isCourseCreatorVerified(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(isVerified, "Verification status retrieved successfully"));
    }

    @Operation(
            summary = "Get verified course creators",
            description = "Fetches a paginated list of all verified course creators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verified course creators retrieved successfully")
            }
    )
    @GetMapping("/verified")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getVerifiedCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> verifiedCourseCreators = courseCreatorService.getVerifiedCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(verifiedCourseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Verified course creators fetched successfully"));
    }

    @Operation(
            summary = "Get unverified course creators",
            description = "Fetches a paginated list of all unverified course creators pending admin review.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Unverified course creators retrieved successfully")
            }
    )
    @GetMapping("/unverified")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorDTO>>> getUnverifiedCourseCreators(
            Pageable pageable) {
        Page<CourseCreatorDTO> unverifiedCourseCreators = courseCreatorService.getUnverifiedCourseCreators(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(unverifiedCourseCreators, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Unverified course creators fetched successfully"));
    }

    @Operation(
            summary = "Get course creator count by verification status",
            description = "Returns the total count of course creators filtered by verification status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
            }
    )
    @GetMapping("/count")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Long>> countCourseCreatorsByVerificationStatus(
            @RequestParam boolean verified) {
        long count = courseCreatorService.countCourseCreatorsByVerificationStatus(verified);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(count, "Course creator count retrieved successfully"));
    }

    // ===== COURSE CREATOR QUALIFICATION DATA =====

    @Operation(summary = "Add skill to course creator", description = "Captures a new competency for a course creator profile.")
    @PostMapping("/{courseCreatorUuid}/skills")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorSkillDTO>> addCourseCreatorSkill(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorSkillDTO skillDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, skillDTO.courseCreatorUuid());
        CourseCreatorSkillDTO createdSkill = courseCreatorSkillService.createCourseCreatorSkill(skillDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdSkill, "Skill added successfully"));
    }

    @Operation(summary = "List course creator skills", description = "Retrieves all recorded skills for a specific course creator.")
    @GetMapping("/{courseCreatorUuid}/skills")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorSkillDTO>>> getCourseCreatorSkills(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<CourseCreatorSkillDTO> skills = courseCreatorSkillService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(skills, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Course creator skills fetched successfully"));
    }

    @Operation(summary = "Update course creator skill", description = "Updates a recorded skill for a course creator.")
    @PutMapping("/{courseCreatorUuid}/skills/{skillUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorSkillDTO>> updateCourseCreatorSkill(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID skillUuid,
            @Valid @RequestBody CourseCreatorSkillDTO skillDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, skillDTO.courseCreatorUuid());
        CourseCreatorSkillDTO updatedSkill = courseCreatorSkillService.updateCourseCreatorSkill(skillUuid, skillDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedSkill, "Skill updated successfully"));
    }

    @Operation(summary = "Delete course creator skill", description = "Removes a skill record from a course creator profile.")
    @DeleteMapping("/{courseCreatorUuid}/skills/{skillUuid}")
    public ResponseEntity<Void> deleteCourseCreatorSkill(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID skillUuid) {
        courseCreatorSkillService.deleteCourseCreatorSkill(skillUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add education record", description = "Captures an academic qualification for a course creator.")
    @PostMapping("/{courseCreatorUuid}/education")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorEducationDTO>> addCourseCreatorEducation(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorEducationDTO educationDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, educationDTO.courseCreatorUuid());
        CourseCreatorEducationDTO createdEducation = courseCreatorEducationService.createCourseCreatorEducation(educationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdEducation, "Education record added successfully"));
    }

    @Operation(summary = "Get course creator education", description = "Retrieves all education history for a course creator.")
    @GetMapping("/{courseCreatorUuid}/education")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorEducationDTO>>> getCourseCreatorEducation(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<CourseCreatorEducationDTO> education = courseCreatorEducationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(education, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Education records fetched successfully"));
    }

    @Operation(summary = "Update education record", description = "Updates an existing course creator education entry.")
    @PutMapping("/{courseCreatorUuid}/education/{educationUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorEducationDTO>> updateCourseCreatorEducation(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID educationUuid,
            @Valid @RequestBody CourseCreatorEducationDTO educationDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, educationDTO.courseCreatorUuid());
        CourseCreatorEducationDTO updatedEducation = courseCreatorEducationService.updateCourseCreatorEducation(educationUuid, educationDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedEducation, "Education record updated successfully"));
    }

    @Operation(summary = "Delete education record", description = "Deletes an education record from a course creator profile.")
    @DeleteMapping("/{courseCreatorUuid}/education/{educationUuid}")
    public ResponseEntity<Void> deleteCourseCreatorEducation(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID educationUuid) {
        courseCreatorEducationService.deleteCourseCreatorEducation(educationUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add experience record", description = "Captures professional experience for a course creator.")
    @PostMapping("/{courseCreatorUuid}/experience")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorExperienceDTO>> addCourseCreatorExperience(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorExperienceDTO experienceDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, experienceDTO.courseCreatorUuid());
        CourseCreatorExperienceDTO createdExperience = courseCreatorExperienceService.createCourseCreatorExperience(experienceDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdExperience, "Experience record added successfully"));
    }

    @Operation(summary = "Get experience history", description = "Retrieves professional experience entries for a course creator.")
    @GetMapping("/{courseCreatorUuid}/experience")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorExperienceDTO>>> getCourseCreatorExperience(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<CourseCreatorExperienceDTO> experience = courseCreatorExperienceService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(experience, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Experience records fetched successfully"));
    }

    @Operation(summary = "Update experience", description = "Updates a recorded work experience entry for a course creator.")
    @PutMapping("/{courseCreatorUuid}/experience/{experienceUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorExperienceDTO>> updateCourseCreatorExperience(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID experienceUuid,
            @Valid @RequestBody CourseCreatorExperienceDTO experienceDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, experienceDTO.courseCreatorUuid());
        CourseCreatorExperienceDTO updatedExperience = courseCreatorExperienceService.updateCourseCreatorExperience(experienceUuid, experienceDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedExperience, "Experience record updated successfully"));
    }

    @Operation(summary = "Delete experience record", description = "Removes a course creator experience entry.")
    @DeleteMapping("/{courseCreatorUuid}/experience/{experienceUuid}")
    public ResponseEntity<Void> deleteCourseCreatorExperience(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID experienceUuid) {
        courseCreatorExperienceService.deleteCourseCreatorExperience(experienceUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add professional membership", description = "Captures an association or membership for a course creator.")
    @PostMapping("/{courseCreatorUuid}/memberships")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorProfessionalMembershipDTO>> addCourseCreatorMembership(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorProfessionalMembershipDTO membershipDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, membershipDTO.courseCreatorUuid());
        CourseCreatorProfessionalMembershipDTO createdMembership = courseCreatorProfessionalMembershipService.createCourseCreatorProfessionalMembership(membershipDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdMembership, "Membership record added successfully"));
    }

    @Operation(summary = "Get professional memberships", description = "Retrieves memberships for a specific course creator.")
    @GetMapping("/{courseCreatorUuid}/memberships")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorProfessionalMembershipDTO>>> getCourseCreatorMemberships(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<CourseCreatorProfessionalMembershipDTO> memberships = courseCreatorProfessionalMembershipService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(memberships, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Membership records fetched successfully"));
    }

    @Operation(summary = "Update membership record", description = "Updates a course creator professional membership.")
    @PutMapping("/{courseCreatorUuid}/memberships/{membershipUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorProfessionalMembershipDTO>> updateCourseCreatorMembership(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID membershipUuid,
            @Valid @RequestBody CourseCreatorProfessionalMembershipDTO membershipDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, membershipDTO.courseCreatorUuid());
        CourseCreatorProfessionalMembershipDTO updatedMembership = courseCreatorProfessionalMembershipService.updateCourseCreatorProfessionalMembership(membershipUuid, membershipDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedMembership, "Membership updated successfully"));
    }

    @Operation(summary = "Delete membership record", description = "Deletes a course creator membership record.")
    @DeleteMapping("/{courseCreatorUuid}/memberships/{membershipUuid}")
    public ResponseEntity<Void> deleteCourseCreatorMembership(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID membershipUuid) {
        courseCreatorProfessionalMembershipService.deleteCourseCreatorProfessionalMembership(membershipUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add certification record", description = "Captures a certification or accreditation held by a course creator.")
    @PostMapping("/{courseCreatorUuid}/certifications")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorCertificationDTO>> addCourseCreatorCertification(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorCertificationDTO certificationDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, certificationDTO.courseCreatorUuid());
        CourseCreatorCertificationDTO createdCertification = courseCreatorCertificationService.createCourseCreatorCertification(certificationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdCertification, "Certification added successfully"));
    }

    @Operation(summary = "Get certifications", description = "Retrieves certification records for a course creator.")
    @GetMapping("/{courseCreatorUuid}/certifications")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorCertificationDTO>>> getCourseCreatorCertifications(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<CourseCreatorCertificationDTO> certifications = courseCreatorCertificationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(certifications, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Certification records fetched successfully"));
    }

    @Operation(summary = "Update certification record", description = "Updates certification metadata for a course creator.")
    @PutMapping("/{courseCreatorUuid}/certifications/{certificationUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorCertificationDTO>> updateCourseCreatorCertification(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID certificationUuid,
            @Valid @RequestBody CourseCreatorCertificationDTO certificationDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, certificationDTO.courseCreatorUuid());
        CourseCreatorCertificationDTO updatedCertification = courseCreatorCertificationService.updateCourseCreatorCertification(certificationUuid, certificationDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCertification, "Certification updated successfully"));
    }

    @Operation(summary = "Delete certification record", description = "Deletes a certification entry from a course creator profile.")
    @DeleteMapping("/{courseCreatorUuid}/certifications/{certificationUuid}")
    public ResponseEntity<Void> deleteCourseCreatorCertification(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID certificationUuid) {
        courseCreatorCertificationService.deleteCourseCreatorCertification(certificationUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== COURSE CREATOR DOCUMENTS =====

    @Operation(summary = "Add document to course creator", description = "Uploads and associates a document with a course creator")
    @PostMapping("/{courseCreatorUuid}/documents")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDocumentDTO>> addCourseCreatorDocument(
            @PathVariable UUID courseCreatorUuid,
            @Valid @RequestBody CourseCreatorDocumentDTO documentDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, documentDTO.courseCreatorUuid());
        CourseCreatorDocumentDTO createdDocument = courseCreatorDocumentService.createCourseCreatorDocument(documentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdDocument, "Document added successfully"));
    }

    @Operation(
            summary = "Upload course creator document file",
            description = """
                    Uploads a PDF document for a course creator and creates a document record.
                    
                    **Use cases:**
                    - Uploading certificates for course creator education records.
                    
                    **File requirements:**
                    - Must be a PDF file (`application/pdf`).
                    - Stored via the platform StorageService under the `profile_documents` folder, partitioned by course creator UUID.
                    """
    )
    @PostMapping(value = "/{courseCreatorUuid}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDocumentDTO>> uploadCourseCreatorDocument(
            @PathVariable UUID courseCreatorUuid,
            @RequestParam("file") @PdfFile MultipartFile file,
            @RequestParam("document_type_uuid") UUID documentTypeUuid,
            @RequestParam(value = "education_uuid", required = false) UUID educationUuid
    ) {
        String folder = storageProperties.getFolders().getProfileDocuments()
                + "/course-creators/" + courseCreatorUuid;
        String storedFileName = storageService.store(file, folder);
        String filePath = storedFileName;
        String originalFilename = file.getOriginalFilename();

        String mimeType = storageService.getContentType(storedFileName);

        CourseCreatorDocumentDTO requestDto = new CourseCreatorDocumentDTO(
                null,
                courseCreatorUuid,
                documentTypeUuid,
                educationUuid,
                originalFilename,
                storedFileName,
                filePath,
                file.getSize(),
                mimeType,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        CourseCreatorDocumentDTO createdDocument = courseCreatorDocumentService.createCourseCreatorDocument(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdDocument, "Document uploaded successfully"));
    }

    @Operation(summary = "Get course creator documents", description = "Retrieves all documents for a specific course creator")
    @GetMapping("/{courseCreatorUuid}/documents")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseCreatorDocumentDTO>>> getCourseCreatorDocuments(
            @PathVariable UUID courseCreatorUuid) {
        List<CourseCreatorDocumentDTO> documents = courseCreatorDocumentService.getDocumentsByCourseCreatorUuid(courseCreatorUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(documents, "Documents fetched successfully"));
    }

    @Operation(summary = "Update course creator document", description = "Updates a specific course creator document")
    @PutMapping("/{courseCreatorUuid}/documents/{documentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDocumentDTO>> updateCourseCreatorDocument(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID documentUuid,
            @Valid @RequestBody CourseCreatorDocumentDTO documentDTO) {
        validateCourseCreatorUuid(courseCreatorUuid, documentDTO.courseCreatorUuid());
        CourseCreatorDocumentDTO updatedDocument = courseCreatorDocumentService.updateCourseCreatorDocument(documentUuid, documentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedDocument, "Document updated successfully"));
    }

    @Operation(summary = "Delete course creator document", description = "Removes a document from a course creator profile")
    @DeleteMapping("/{courseCreatorUuid}/documents/{documentUuid}")
    public ResponseEntity<Void> deleteCourseCreatorDocument(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID documentUuid) {
        courseCreatorDocumentService.deleteCourseCreatorDocument(documentUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify course creator document", description = "Marks a course creator document as verified")
    @PostMapping("/{courseCreatorUuid}/documents/{documentUuid}/verify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseCreatorDocumentDTO>> verifyCourseCreatorDocument(
            @PathVariable UUID courseCreatorUuid,
            @PathVariable UUID documentUuid,
            @RequestParam String verifiedBy,
            @RequestParam(required = false) String verificationNotes) {
        CourseCreatorDocumentDTO verifiedDocument = courseCreatorDocumentService
                .verifyCourseCreatorDocument(documentUuid, verifiedBy, verificationNotes);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(verifiedDocument, "Document verified successfully"));
    }

    // ===== QUALIFICATION SEARCH =====

    @Operation(summary = "Search course creator skills", description = "Advanced search endpoint for course creator skills using query parameters.")
    @GetMapping("/skills/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorSkillDTO>>> searchCourseCreatorSkills(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorSkillDTO> results = courseCreatorSkillService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(results, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Skill search completed successfully"));
    }

    @Operation(summary = "Search course creator education", description = "Advanced search endpoint for course creator education history.")
    @GetMapping("/education/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorEducationDTO>>> searchCourseCreatorEducation(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorEducationDTO> results = courseCreatorEducationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(results, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Education search completed successfully"));
    }

    @Operation(summary = "Search course creator experience", description = "Advanced search endpoint for course creator experience history.")
    @GetMapping("/experience/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorExperienceDTO>>> searchCourseCreatorExperience(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorExperienceDTO> results = courseCreatorExperienceService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(results, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Experience search completed successfully"));
    }

    @Operation(summary = "Search course creator memberships", description = "Advanced search endpoint for course creator memberships.")
    @GetMapping("/memberships/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorProfessionalMembershipDTO>>> searchCourseCreatorMemberships(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorProfessionalMembershipDTO> results = courseCreatorProfessionalMembershipService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(results, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Membership search completed successfully"));
    }

    @Operation(summary = "Search course creator certifications", description = "Advanced search endpoint for course creator certifications.")
    @GetMapping("/certifications/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CourseCreatorCertificationDTO>>> searchCourseCreatorCertifications(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CourseCreatorCertificationDTO> results = courseCreatorCertificationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(results, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                        "Certification search completed successfully"));
    }

    private void validateCourseCreatorUuid(UUID pathUuid, UUID payloadUuid) {
        if (payloadUuid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course_creator_uuid is required in the payload");
        }
        if (!payloadUuid.equals(pathUuid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course creator UUID mismatch between path and payload");
        }
    }
}
