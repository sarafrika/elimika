package apps.sarafrika.elimika.instructor.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.instructor.dto.*;
import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.service.*;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.utils.validation.PdfFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
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

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing instructor operations and related entities.
 */
@RestController
@RequestMapping(InstructorController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Instructor Management", description = "Comprehensive endpoints for managing instructors and related entities")
public class InstructorController {

    public static final String API_ROOT_PATH = "/api/v1/instructors";

    private final InstructorService instructorService;
    private final InstructorDocumentService instructorDocumentService;
    private final InstructorEducationService instructorEducationService;
    private final InstructorExperienceService instructorExperienceService;
    private final InstructorProfessionalMembershipService instructorProfessionalMembershipService;
    private final InstructorSkillService instructorSkillService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final InstructorReviewService instructorReviewService;

    // ===== INSTRUCTOR BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new instructor",
            description = "Saves a new instructor record in the system.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Instructor created successfully",
                            content = @Content(schema = @Schema(implementation = InstructorDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDTO>> createInstructor(@Valid @RequestBody InstructorDTO instructorDTO) {
        InstructorDTO createdInstructor = instructorService.createInstructor(instructorDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdInstructor, "Instructor created successfully"));
    }

    @Operation(
            summary = "Get instructor by UUID",
            description = "Fetches an instructor by their UUID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Instructor found",
                            content = @Content(schema = @Schema(implementation = InstructorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDTO>> getInstructorByUuid(@PathVariable UUID uuid) {
        InstructorDTO instructorDTO = instructorService.getInstructorByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(instructorDTO, "Instructor record fetched successfully"));
    }

    @Operation(
            summary = "Get all instructors",
            description = "Fetches a paginated list of instructors."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorDTO>>> getAllInstructors(Pageable pageable) {
        Page<InstructorDTO> instructors = instructorService.getAllInstructors(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(instructors, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Instructors fetched successfully"));
    }

    @Operation(
            summary = "Update an instructor",
            description = "Updates an existing instructor record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Instructor updated successfully",
                            content = @Content(schema = @Schema(implementation = InstructorDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDTO>> updateInstructor(
            @PathVariable UUID uuid,
            @Valid @RequestBody InstructorDTO instructorDTO) {
        InstructorDTO updatedInstructor = instructorService.updateInstructor(uuid, instructorDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedInstructor, "Instructor updated successfully"));
    }

    @Operation(
            summary = "Delete an instructor",
            description = "Removes an instructor record from the system.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Instructor deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Instructor not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteInstructor(@PathVariable UUID uuid) {
        instructorService.deleteInstructor(uuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search instructors",
            description = """
                    Search for instructors using flexible criteria with advanced operators.
                   \s
                    **Basic Search:**
                    - `field=value` - Exact match (default operation)
                    - `firstName=John` - Find instructors with firstName exactly "John"
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
                    - `lastName_like=smith` - Last name contains "smith"
                   \s
                    **List Operations:**
                    - `field_in=val1,val2,val3` - Field is in list
                    - `field_notin=val1,val2` - Field is not in list
                    - `status_in=ACTIVE,PENDING` - Status is either ACTIVE or PENDING
                   \s
                    **Negation:**
                    - `field_noteq=value` - Not equal to value
                    - `isActive_noteq=false` - Is not false (i.e., is true)
                   \s
                    **Range Operations:**
                    - `field_between=start,end` - Value between start and end (inclusive)
                    - `createdDate_between=2024-01-01T00:00:00,2024-12-31T23:59:59` - Created in 2024
                   \s
                    **Complex Operations:**
                    - `field_notingroup=relationshipField,groupId` - Not in specific group
                   \s
                    **Nested Field Access:**
                    - `nestedObject.field=value` - Search in nested objects
                   \s
                    **Supported Data Types:**
                    - String, UUID, Boolean (true/false or 1/0), Integer, Long, Double, Float, BigDecimal
                    - Date (YYYY-MM-DD), Timestamp, LocalDateTime (ISO format)
                   \s
                    **Examples:**
                    - `/search?firstName_like=john&isActive=true&createdDate_gte=2024-01-01T00:00:00`
                    - `/search?experience_gt=5&status_in=ACTIVE,VERIFIED`
                    - `/search?email_endswith=@company.com&department_noteq=IT`
                   \s""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = Page.class)))
            }
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorDTO>>> searchInstructors(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorDTO> instructors = instructorService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(instructors, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Instructor search successful"));
    }

    // ===== INSTRUCTOR DOCUMENTS =====

    @Operation(summary = "Add document to instructor", description = "Uploads and associates a document with an instructor")
    @PostMapping("/{instructorUuid}/documents")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDocumentDTO>> addInstructorDocument(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorDocumentDTO documentDTO) {

        InstructorDocumentDTO createdDocument = instructorDocumentService.createInstructorDocument(documentDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdDocument, "Document added successfully"));
    }

    @Operation(
            summary = "Upload instructor document file",
            description = """
                    Uploads a PDF document for an instructor and creates a document record.
                    
                    **Use cases:**
                    - Uploading certificates, licenses, and other professional credentials.
                    - Attaching supporting documents to education, experience, or membership records.
                    
                    **File requirements:**
                    - Must be a PDF file (`application/pdf`).
                    - Stored via the platform StorageService under the `profile_documents` folder, partitioned by instructor UUID.
                    """
    )
    @PostMapping(value = "/{instructorUuid}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDocumentDTO>> uploadInstructorDocument(
            @PathVariable UUID instructorUuid,
            @RequestParam("file") @PdfFile MultipartFile file,
            @RequestParam("document_type_uuid") UUID documentTypeUuid,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "education_uuid", required = false) UUID educationUuid,
            @RequestParam(value = "experience_uuid", required = false) UUID experienceUuid,
            @RequestParam(value = "membership_uuid", required = false) UUID membershipUuid,
            @RequestParam(value = "expiry_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate
    ) {

        String folder = storageProperties.getFolders().getProfileDocuments()
                + "/instructors/" + instructorUuid;

        String storedFileName = storageService.store(file, folder);
        String filePath = storedFileName;
        String originalFilename = file.getOriginalFilename();
        String resolvedTitle = (title != null && !title.isBlank())
                ? title
                : (originalFilename != null ? originalFilename : "Instructor Document");

        String mimeType = storageService.getContentType(storedFileName);

        InstructorDocumentDTO requestDto = new InstructorDocumentDTO(
                null,
                instructorUuid,
                documentTypeUuid,
                educationUuid,
                experienceUuid,
                membershipUuid,
                originalFilename,
                storedFileName,
                filePath,
                file.getSize(),
                mimeType,
                null,
                resolvedTitle,
                description,
                null,
                null,
                null,
                null,
                null,
                null,
                expiryDate,
                null,
                null,
                null,
                null
        );

        InstructorDocumentDTO createdDocument = instructorDocumentService.createInstructorDocument(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdDocument, "Document uploaded successfully"));
    }

    @Operation(summary = "Get instructor documents", description = "Retrieves all documents for a specific instructor")
    @GetMapping("/{instructorUuid}/documents")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<InstructorDocumentDTO>>> getInstructorDocuments(
            @PathVariable UUID instructorUuid) {
        List<InstructorDocumentDTO> documents = instructorDocumentService.getDocumentsByInstructorUuid(instructorUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(documents, "Documents fetched successfully"));
    }

    @Operation(summary = "Update instructor document", description = "Updates a specific document")
    @PutMapping("/{instructorUuid}/documents/{documentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDocumentDTO>> updateInstructorDocument(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID documentUuid,
            @Valid @RequestBody InstructorDocumentDTO documentDTO) {
        InstructorDocumentDTO updatedDocument = instructorDocumentService.updateInstructorDocument(documentUuid, documentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedDocument, "Document updated successfully"));
    }

    @Operation(summary = "Delete instructor document", description = "Removes a document from an instructor")
    @DeleteMapping("/{instructorUuid}/documents/{documentUuid}")
    public ResponseEntity<Void> deleteInstructorDocument(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID documentUuid) {
        instructorDocumentService.deleteInstructorDocument(documentUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify instructor document", description = "Marks a document as verified")
    @PostMapping("/{instructorUuid}/documents/{documentUuid}/verify")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorDocumentDTO>> verifyDocument(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID documentUuid,
            @RequestParam String verifiedBy,
            @RequestParam(required = false) String verificationNotes) {
        InstructorDocumentDTO verifiedDocument = instructorDocumentService.verifyDocument(documentUuid, verifiedBy, verificationNotes);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(verifiedDocument, "Document verified successfully"));
    }

    // ===== INSTRUCTOR REVIEWS =====

    @Operation(
            summary = "Submit a review for an instructor",
            description = """
                    Allows a student to leave a review for an instructor, scoped to a specific enrollment.
                    
                    Frontend clients should:
                    - Use the student's enrollment UUID and the instructor UUID for the class they attended.
                    - Enforce that each enrollment can create at most one review for a given instructor.
                    """
    )
    @PostMapping("/{instructorUuid}/reviews")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorReviewDTO>> submitInstructorReview(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorReviewDTO reviewDTO) {

        InstructorReviewDTO payload = new InstructorReviewDTO(
                null,
                instructorUuid,
                reviewDTO.studentUuid(),
                reviewDTO.enrollmentUuid(),
                reviewDTO.rating(),
                reviewDTO.headline(),
                reviewDTO.comments(),
                reviewDTO.clarityRating(),
                reviewDTO.engagementRating(),
                reviewDTO.punctualityRating(),
                reviewDTO.isAnonymous(),
                null,
                null,
                null,
                null
        );

        InstructorReviewDTO created = instructorReviewService.createReview(payload);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(created, "Review submitted successfully"));
    }

    @Operation(
            summary = "Get reviews for an instructor",
            description = "Returns all reviews left for the specified instructor."
    )
    @GetMapping("/{instructorUuid}/reviews")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<InstructorReviewDTO>>> getInstructorReviews(
            @PathVariable UUID instructorUuid) {
        List<InstructorReviewDTO> reviews = instructorReviewService.getReviewsForInstructor(instructorUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(reviews, "Instructor reviews fetched successfully"));
    }

    @Operation(
            summary = "Get instructor rating summary",
            description = "Returns average rating and total review count for an instructor."
    )
    @GetMapping("/{instructorUuid}/reviews/summary")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorRatingSummaryDTO>> getInstructorRatingSummary(
            @PathVariable UUID instructorUuid) {
        List<InstructorReviewDTO> reviews = instructorReviewService.getReviewsForInstructor(instructorUuid);
        long count = reviews.size();

        Double average = null;
        if (count > 0) {
            average = reviews.stream()
                    .map(InstructorReviewDTO::rating)
                    .filter(r -> r != null)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(Double.NaN);
            if (average.isNaN()) {
                average = null;
            }
        }

        InstructorRatingSummaryDTO summary = new InstructorRatingSummaryDTO(
                instructorUuid,
                average,
                count
        );

        return ResponseEntity.ok(
                apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(summary, "Instructor rating summary fetched successfully")
        );
    }

    // ===== INSTRUCTOR EDUCATION =====

    @Operation(summary = "Add education to instructor", description = "Adds educational qualification to an instructor")
    @PostMapping("/{instructorUuid}/education")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorEducationDTO>> addInstructorEducation(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorEducationDTO educationDTO) {

        InstructorEducationDTO createdEducation = instructorEducationService.createInstructorEducation(educationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdEducation, "Education record added successfully"));
    }

    @Operation(summary = "Get instructor education", description = "Retrieves all education records for a specific instructor")
    @GetMapping("/{instructorUuid}/education")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<InstructorEducationDTO>>> getInstructorEducation(
            @PathVariable UUID instructorUuid) {
        List<InstructorEducationDTO> education = instructorEducationService.getEducationByInstructorUuid(instructorUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(education, "Education records fetched successfully"));
    }

    @Operation(summary = "Update instructor education", description = "Updates a specific education record")
    @PutMapping("/{instructorUuid}/education/{educationUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorEducationDTO>> updateInstructorEducation(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID educationUuid,
            @Valid @RequestBody InstructorEducationDTO educationDTO) {
        InstructorEducationDTO updatedEducation = instructorEducationService.updateInstructorEducation(educationUuid, educationDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedEducation, "Education record updated successfully"));
    }

    @Operation(summary = "Delete instructor education", description = "Removes an education record from an instructor")
    @DeleteMapping("/{instructorUuid}/education/{educationUuid}")
    public ResponseEntity<Void> deleteInstructorEducation(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID educationUuid) {
        instructorEducationService.deleteInstructorEducation(educationUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== INSTRUCTOR EXPERIENCE =====

    @Operation(summary = "Add experience to instructor", description = "Adds work experience to an instructor")
    @PostMapping("/{instructorUuid}/experience")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorExperienceDTO>> addInstructorExperience(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorExperienceDTO experienceDTO) {

        InstructorExperienceDTO createdExperience = instructorExperienceService.createInstructorExperience(experienceDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdExperience, "Experience record added successfully"));
    }

    @Operation(summary = "Get instructor experience", description = "Retrieves all experience records for a specific instructor")
    @GetMapping("/{instructorUuid}/experience")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorExperienceDTO>>> getInstructorExperience(
            @PathVariable UUID instructorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("instructorUuid", instructorUuid.toString());
        Page<InstructorExperienceDTO> experience = instructorExperienceService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(experience, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Experience records fetched successfully"));
    }

    @Operation(summary = "Update instructor experience", description = "Updates a specific experience record")
    @PutMapping("/{instructorUuid}/experience/{experienceUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorExperienceDTO>> updateInstructorExperience(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID experienceUuid,
            @Valid @RequestBody InstructorExperienceDTO experienceDTO) {
        InstructorExperienceDTO updatedExperience = instructorExperienceService.updateInstructorExperience(experienceUuid, experienceDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedExperience, "Experience record updated successfully"));
    }

    @Operation(summary = "Delete instructor experience", description = "Removes an experience record from an instructor")
    @DeleteMapping("/{instructorUuid}/experience/{experienceUuid}")
    public ResponseEntity<Void> deleteInstructorExperience(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID experienceUuid) {
        instructorExperienceService.deleteInstructorExperience(experienceUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== INSTRUCTOR PROFESSIONAL MEMBERSHIPS =====

    @Operation(summary = "Add membership to instructor", description = "Adds professional membership to an instructor")
    @PostMapping("/{instructorUuid}/memberships")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorProfessionalMembershipDTO>> addInstructorMembership(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorProfessionalMembershipDTO membershipDTO) {

        InstructorProfessionalMembershipDTO createdMembership = instructorProfessionalMembershipService
                .createInstructorProfessionalMembership(membershipDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdMembership, "Membership record added successfully"));
    }

    @Operation(summary = "Get instructor memberships", description = "Retrieves all membership records for a specific instructor")
    @GetMapping("/{instructorUuid}/memberships")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorProfessionalMembershipDTO>>> getInstructorMemberships(
            @PathVariable UUID instructorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("instructorUuid", instructorUuid.toString());
        Page<InstructorProfessionalMembershipDTO> memberships = instructorProfessionalMembershipService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(memberships, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Membership records fetched successfully"));
    }

    @Operation(summary = "Update instructor membership", description = "Updates a specific membership record")
    @PutMapping("/{instructorUuid}/memberships/{membershipUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorProfessionalMembershipDTO>> updateInstructorMembership(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID membershipUuid,
            @Valid @RequestBody InstructorProfessionalMembershipDTO membershipDTO) {
        InstructorProfessionalMembershipDTO updatedMembership = instructorProfessionalMembershipService.updateInstructorProfessionalMembership(membershipUuid, membershipDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedMembership, "Membership record updated successfully"));
    }

    @Operation(summary = "Delete instructor membership", description = "Removes a membership record from an instructor")
    @DeleteMapping("/{instructorUuid}/memberships/{membershipUuid}")
    public ResponseEntity<Void> deleteInstructorMembership(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID membershipUuid) {
        instructorProfessionalMembershipService.deleteInstructorProfessionalMembership(membershipUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== INSTRUCTOR SKILLS =====

    @Operation(summary = "Add skill to instructor", description = "Adds a skill to an instructor")
    @PostMapping("/{instructorUuid}/skills")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorSkillDTO>> addInstructorSkill(
            @PathVariable UUID instructorUuid,
            @Valid @RequestBody InstructorSkillDTO skillDTO) {

        InstructorSkillDTO createdSkill = instructorSkillService.createInstructorSkill(skillDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdSkill, "Skill added successfully"));
    }

    @Operation(summary = "Get instructor skills", description = "Retrieves all skills for a specific instructor")
    @GetMapping("/{instructorUuid}/skills")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorSkillDTO>>> getInstructorSkills(
            @PathVariable UUID instructorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("instructorUuid", instructorUuid.toString());
        Page<InstructorSkillDTO> skills = instructorSkillService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(skills, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Skills fetched successfully"));
    }

    @Operation(summary = "Update instructor skill", description = "Updates a specific skill record")
    @PutMapping("/{instructorUuid}/skills/{skillUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<InstructorSkillDTO>> updateInstructorSkill(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID skillUuid,
            @Valid @RequestBody InstructorSkillDTO skillDTO) {
        InstructorSkillDTO updatedSkill = instructorSkillService.updateInstructorSkill(skillUuid, skillDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedSkill, "Skill updated successfully"));
    }

    @Operation(summary = "Delete instructor skill", description = "Removes a skill from an instructor")
    @DeleteMapping("/{instructorUuid}/skills/{skillUuid}")
    public ResponseEntity<Void> deleteInstructorSkill(
            @PathVariable UUID instructorUuid,
            @PathVariable UUID skillUuid) {
        instructorSkillService.deleteInstructorSkill(skillUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== SEARCH ENDPOINTS FOR RELATED ENTITIES =====

    @Operation(
            summary = "Search instructor documents",
            description = """
                    Search documents with flexible criteria using advanced operators.
                    
                    **Common Document Search Examples:**
                    - `instructorUuid=uuid` - All documents for specific instructor
                    - `isVerified=false` - Unverified documents
                    - `status=PENDING` - Documents with pending status
                    - `status_in=APPROVED,VERIFIED` - Approved or verified documents
                    - `expiryDate_lte=2025-12-31` - Documents expiring by end of 2025
                    - `mimeType_like=pdf` - PDF documents
                    - `fileSizeBytes_gt=1048576` - Files larger than 1MB
                    - `title_startswith=Certificate` - Titles starting with "Certificate"
                    - `createdDate_between=2024-01-01T00:00:00,2024-12-31T23:59:59` - Created in 2024
                    
                    **Special Document Queries:**
                    - `isVerified=false&expiryDate_lte=2025-12-31` - Unverified expiring documents
                    - `status_noteq=EXPIRED&expiryDate_lt=2025-07-02` - Non-expired but overdue docs
                    
                    For complete operator documentation, see the main search endpoint.
                    """
    )
    @GetMapping("/documents/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorDocumentDTO>>> searchDocuments(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorDocumentDTO> documents = instructorDocumentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(documents, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Document search completed successfully"));
    }

    @Operation(
            summary = "Search instructor education",
            description = """
                    Search education records with flexible criteria.
                    
                    **Common Education Search Examples:**
                    - `instructorUuid=uuid` - All education for specific instructor
                    - `qualification_like=degree` - Qualifications containing "degree"
                    - `schoolName_startswith=University` - Schools starting with "University"
                    - `yearCompleted_gte=2020` - Completed in 2020 or later
                    - `yearCompleted_between=2015,2020` - Completed between 2015-2020
                    - `certificateNumber_noteq=null` - Has certificate number
                    
                    For complete operator documentation, see the main search endpoint.
                    """
    )
    @GetMapping("/education/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorEducationDTO>>> searchEducation(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorEducationDTO> education = instructorEducationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(education, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Education search completed successfully"));
    }

    @Operation(
            summary = "Search instructor experience",
            description = """
                    Search experience records with flexible criteria.
                    
                    **Common Experience Search Examples:**
                    - `instructorUuid=uuid` - All experience for specific instructor
                    - `isCurrentPosition=true` - Current positions only
                    - `position_like=manager` - Positions containing "manager"
                    - `organizationName_endswith=Ltd` - Organizations ending with "Ltd"
                    - `yearsOfExperience_gte=5` - 5+ years experience
                    - `startDate_gte=2020-01-01` - Started in 2020 or later
                    - `endDate=null` - Ongoing positions (no end date)
                    - `responsibilities_like=team` - Responsibilities mentioning "team"
                    
                    **Experience Analysis Queries:**
                    - `isCurrentPosition=false&endDate_gte=2023-01-01` - Recent past positions
                    - `yearsOfExperience_between=3,10` - Mid-level experience (3-10 years)
                    
                    For complete operator documentation, see the main search endpoint.
                    """
    )
    @GetMapping("/experience/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorExperienceDTO>>> searchExperience(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorExperienceDTO> experience = instructorExperienceService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(experience, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Experience search completed successfully"));
    }

    @Operation(
            summary = "Search instructor memberships",
            description = """
                    Search membership records with flexible criteria.
                    
                    **Common Membership Search Examples:**
                    - `instructorUuid=uuid` - All memberships for specific instructor
                    - `isActive=true` - Active memberships only
                    - `organizationName_like=professional` - Organizations with "professional" in name
                    - `startDate_gte=2023-01-01` - Memberships started in 2023 or later
                    - `endDate=null` - Ongoing memberships (no end date)
                    - `membershipNumber_startswith=PRO` - Numbers starting with "PRO"
                    
                    **Membership Analysis Queries:**
                    - `isActive=true&endDate=null` - Currently active ongoing memberships
                    - `isActive=false&endDate_gte=2024-01-01` - Recently expired memberships
                    - `startDate_between=2020-01-01,2023-12-31` - Joined between 2020-2023
                    
                    For complete operator documentation, see the main search endpoint.
                    """
    )
    @GetMapping("/memberships/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorProfessionalMembershipDTO>>> searchMemberships(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorProfessionalMembershipDTO> memberships = instructorProfessionalMembershipService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(memberships, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Membership search completed successfully"));
    }

    @Operation(
            summary = "Search instructor skills",
            description = """
                    Search skills with flexible criteria.
                    
                    **Common Skills Search Examples:**
                    - `instructorUuid=uuid` - All skills for specific instructor
                    - `skillName_like=java` - Skills containing "java"
                    - `proficiencyLevel=EXPERT` - Expert level skills only
                    - `proficiencyLevel_in=ADVANCED,EXPERT` - Advanced or expert skills
                    - `skillName_startswith=Data` - Skills starting with "Data"
                    - `proficiencyLevel_noteq=BEGINNER` - Non-beginner skills
                    
                    **Skills Analysis Queries:**
                    - `skillName_like=programming&proficiencyLevel_in=ADVANCED,EXPERT` - Advanced programming skills
                    - `createdDate_gte=2024-01-01&proficiencyLevel=EXPERT` - Recently added expert skills
                    
                    **Proficiency Levels:** BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
                    
                    For complete operator documentation, see the main search endpoint.
                    """
    )
    @GetMapping("/skills/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<InstructorSkillDTO>>> searchSkills(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<InstructorSkillDTO> skills = instructorSkillService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(skills, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Skills search completed successfully"));
    }
}
