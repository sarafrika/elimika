package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.shared.storage.util.MediaOwnerType;
import apps.sarafrika.elimika.shared.storage.util.StoragePathUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for comprehensive certificate management and verification.
 * <p>
 * <strong>Who may do what here.</strong> A certificate is an assertion the platform makes about
 * somebody's achievement, and it carries their final grade. Minting one, attaching a PDF to it,
 * pointing it at a download URL or revoking it are therefore all the same right — the right to
 * grade the work it attests to — and are limited to the staff behind the course or program plus
 * platform admins. Reading one is limited to the learner it names, that staff, a manager of an
 * organisation the learner belongs to, and platform admins. Platform-wide listings (every
 * certificate, every revocation, arbitrary search) are admin-only, because each of them returns
 * other people's grades in bulk.
 * <p>
 * Two routes are deliberately wider, and both are named in {@code SecurityConfiguration} as
 * {@code permitAll} so that they really are reachable without a token — everything else on this
 * controller falls under {@code anyRequest().authenticated()}. {@code GET /verify/{number}}
 * answers only "valid or not" and is the route a stranger holding a printed certificate is meant
 * to use. {@code GET /files/**} serves the stored PDF, alongside the platform's other stored
 * media, so that download links in dashboards and e-mail work without a token.
 */
@RestController
@RequestMapping(CertificateController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Certificate Management", description = "Complete certificate lifecycle including generation, verification, and templates")
public class CertificateController {

    public static final String API_ROOT_PATH = "/api/v1/certificates";

    /**
     * Issuance, amendment and revocation of an existing certificate.
     */
    private static final String MANAGE_CERTIFICATE =
            "@domainSecurityService.isPlatformAdmin() or @courseSecurityService.canManageCertificate(#uuid)";

    /**
     * Reading one certificate in full, grade included.
     */
    private static final String READ_CERTIFICATE =
            "@domainSecurityService.isPlatformAdmin() or @courseSecurityService.canReadCertificate(#uuid)";

    /**
     * Reading a learner's certificates as a list.
     */
    private static final String READ_STUDENT_CERTIFICATES =
            "@domainSecurityService.isPlatformAdmin() or @courseSecurityService.canReadStudentCertificates(#studentUuid)";

    /**
     * Platform-wide listings and the destructive operations, which no single course or organisation
     * is the right owner of.
     */
    private static final String PLATFORM_ADMIN = "@domainSecurityService.isPlatformAdmin()";

    /**
     * Certificate templates are shared, unowned presentation assets, so authoring one is a staff
     * act rather than a course-scoped one.
     */
    private static final String TEACHING_STAFF =
            "@domainSecurityService.isInstructorOrAdmin() or @domainSecurityService.isCourseCreator()";

    private final CertificateService certificateService;
    private final CertificateTemplateService certificateTemplateService;
    private final MediaStorageService mediaStorageService;
    private final MediaServeService mediaServeService;
    private final StorageProperties storageProperties;

    // ===== CERTIFICATE BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new certificate",
            description = "Manually creates a certificate record with automatic number generation. "
                    + "The body names either a course or a program - never both, and never neither - "
                    + "and the caller must be entitled to grade whichever one it names.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Certificate created successfully",
                            content = @Content(schema = @Schema(implementation = CertificateDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() or @courseSecurityService"
            + ".canAwardCertificate(#certificateDTO.courseUuid(), #certificateDTO.programUuid())")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> createCertificate(
            @Valid @RequestBody CertificateDTO certificateDTO) {
        CertificateDTO createdCertificate = certificateService.createCertificate(certificateDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdCertificate, "Certificate created successfully"));
    }

    @Operation(
            summary = "Upload certificate PDF",
            description = """
                    Uploads an externally generated certificate PDF file for an existing certificate record and updates its download URL.
                    
                    **File requirements:**
                    - Must be a PDF (`application/pdf`).
                    - Stored via the platform StorageService under the `certificates` folder.
                    
                    Frontend clients should call this after a certificate record exists, then use the returned `certificate_url`
                    to power download links in student dashboards and admin UIs.
                    """
    )
    @PostMapping(value = "/{uuid}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(MANAGE_CERTIFICATE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> uploadCertificatePdf(
            @PathVariable UUID uuid,
            @RequestParam("file") @PdfFile MultipartFile file
    ) {
        CertificateDTO existing = certificateService.getCertificateByUuid(uuid);

        String fileUrl = mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.PDF_DOCUMENT,
                storageProperties.getFolders().getCertificates(),
                MediaOwnerType.CERTIFICATE, uuid,
                existing.certificateUrl()
        )).key();

        CertificateDTO updatePayload = new CertificateDTO(
                existing.uuid(),
                existing.certificateNumber(),
                existing.studentUuid(),
                existing.courseUuid(),
                existing.programUuid(),
                existing.templateUuid(),
                existing.issuedDate(),
                existing.completionDate(),
                existing.finalGrade(),
                fileUrl,
                existing.isValid(),
                existing.revokedAt(),
                existing.revokedReason(),
                existing.createdDate(),
                existing.createdBy(),
                existing.updatedDate(),
                existing.updatedBy()
        );

        CertificateDTO updated = certificateService.updateCertificate(uuid, updatePayload);
        return ResponseEntity.ok(
                apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(updated, "Certificate PDF uploaded successfully")
        );
    }

    /**
     * Left unauthenticated on purpose: {@code SecurityConfiguration} permits
     * {@code GET /api/v1/certificates/files/**} along with the platform's other stored media, so
     * that the download links dashboards and e-mails hand a learner work without a token. Access is
     * by unguessable storage key rather than by identity; a guard here would silently break every
     * one of those links.
     */
    @Operation(
            summary = "Get certificate PDF by file path",
            description = "Retrieves a certificate PDF by its stored relative path. Public, like the platform's other stored media."
    )
    @GetMapping("/files/{*filePath}")
    public ResponseEntity<Resource> getCertificateFile(
            @PathVariable String filePath
    ) {
        // Legacy double-nested URLs may carry a duplicated leading "certificates/";
        // fall back to the deduplicated key when the literal path is absent.
        String normalized = StoragePathUtils.normalizeRelativePath(filePath);
        String fallback = null;
        String certificatesFolder = storageProperties.getFolders().getCertificates() + "/";
        if (normalized != null && normalized.startsWith(certificatesFolder + certificatesFolder)) {
            fallback = normalized.substring(certificatesFolder.length());
        } else if (normalized != null && !normalized.contains("/")) {
            fallback = certificatesFolder + normalized;
        }
        return mediaServeService.serve(normalized, fallback);
    }

    @Operation(
            summary = "Get certificate by UUID",
            description = "Retrieves a complete certificate including computed properties and verification status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Certificate found"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @GetMapping("/{uuid}")
    @PreAuthorize(READ_CERTIFICATE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> getCertificateByUuid(
            @PathVariable UUID uuid) {
        CertificateDTO certificateDTO = certificateService.getCertificateByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(certificateDTO, "Certificate retrieved successfully"));
    }

    @Operation(
            summary = "Get all certificates",
            description = "Retrieves paginated list of all certificates with filtering support. Platform administrators only."
    )
    @GetMapping
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateDTO>>> getAllCertificates(
            Pageable pageable) {
        Page<CertificateDTO> certificates = certificateService.getAllCertificates(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(certificates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Certificates retrieved successfully"));
    }

    /**
     * The guard authorises against the certificate as it currently stands, so what the certificate
     * attests to must not move underneath it: the service refuses a body that re-points the
     * student, course or program. Without that, staff entitled to course A could PUT course B onto
     * a record and be re-authorised on the result.
     */
    @Operation(
            summary = "Update certificate",
            description = "Updates an existing certificate with selective field updates. The student, course "
                    + "and program a certificate attests to are fixed at issue; correct a wrong certificate by "
                    + "revoking it and issuing a new one.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Certificate updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Attempted to re-point the certificate's subject"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @PutMapping("/{uuid}")
    @PreAuthorize(MANAGE_CERTIFICATE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> updateCertificate(
            @PathVariable UUID uuid,
            @Valid @RequestBody CertificateDTO certificateDTO) {
        CertificateDTO updatedCertificate = certificateService.updateCertificate(uuid, certificateDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCertificate, "Certificate updated successfully"));
    }

    @Operation(
            summary = "Delete certificate",
            description = "Permanently removes a certificate record. Platform administrators only - "
                    + "course staff withdraw a certificate by revoking it, which leaves the record and its reason behind.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Certificate deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @DeleteMapping("/{uuid}")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<Void> deleteCertificate(@PathVariable UUID uuid) {
        certificateService.deleteCertificate(uuid);
        return ResponseEntity.noContent().build();
    }

    // ===== CERTIFICATE GENERATION =====

    @Operation(
            summary = "Generate course certificate",
            description = "Automatically generates a certificate upon course completion.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Certificate generated successfully"),
                    @ApiResponse(responseCode = "400", description = "Student not eligible for certificate")
            }
    )
    @PostMapping("/generate/course")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() or @courseSecurityService.canAwardCertificate(#courseUuid, null)")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> generateCourseCertificate(
            @RequestParam UUID studentUuid,
            @RequestParam UUID courseUuid,
            @RequestParam BigDecimal finalGrade) {
        if (!certificateService.isEligibleForCourseCertificate(studentUuid, courseUuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                            .error("Student is not eligible for course certificate", null));
        }

        CertificateDTO certificate = certificateService.generateCourseCertificate(studentUuid, courseUuid, finalGrade);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(certificate, "Course certificate generated successfully"));
    }

    @Operation(
            summary = "Generate program certificate",
            description = "Automatically generates a certificate upon program completion.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Certificate generated successfully"),
                    @ApiResponse(responseCode = "400", description = "Student not eligible for certificate")
            }
    )
    @PostMapping("/generate/program")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() or @courseSecurityService.canAwardCertificate(null, #programUuid)")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> generateProgramCertificate(
            @RequestParam UUID studentUuid,
            @RequestParam UUID programUuid,
            @RequestParam BigDecimal finalGrade) {
        if (!certificateService.isEligibleForProgramCertificate(studentUuid, programUuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                            .error("Student is not eligible for program certificate", null));
        }

        CertificateDTO certificate = certificateService.generateProgramCertificate(studentUuid, programUuid, finalGrade);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(certificate, "Program certificate generated successfully"));
    }

    // ===== CERTIFICATE VERIFICATION =====

    /**
     * The verification route proper, and the only certificate route open to anyone holding a
     * number rather than a relationship to the learner: {@code SecurityConfiguration} permits
     * {@code GET /api/v1/certificates/verify/*} unauthenticated. It answers a single boolean and
     * discloses no grade, no learner and no course, so a stranger checking a printed certificate
     * learns exactly what they came for and nothing else.
     */
    @Operation(
            summary = "Verify certificate",
            description = "Verifies the authenticity of a certificate using its certificate number. "
                    + "Returns validity only - use this, not the by-number lookup, for third-party verification.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Certificate verification result")
            }
    )
    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Boolean>> verifyCertificate(
            @PathVariable String certificateNumber) {
        boolean isValid = certificateService.verifyCertificate(certificateNumber);
        String message = isValid ? "Certificate is valid" : "Certificate is invalid or revoked";
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(isValid, message));
    }

    @Operation(
            summary = "Get certificate by number",
            description = "Retrieves full certificate details using the certificate number. This returns the "
                    + "learner's final grade, so it is guarded like any other read; third parties verifying a "
                    + "printed certificate should use the verification endpoint instead."
    )
    @GetMapping("/number/{certificateNumber}")
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() "
            + "or @courseSecurityService.canReadCertificateByNumber(#certificateNumber)")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> getCertificateByNumber(
            @PathVariable String certificateNumber) {
        CertificateDTO certificate = certificateService.getCertificateByNumber(certificateNumber);
        if (certificate == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(certificate, "Certificate found"));
    }

    // ===== CERTIFICATE MANAGEMENT =====

    @Operation(
            summary = "Revoke certificate",
            description = "Revokes a certificate with reason, making it invalid.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Certificate revoked successfully"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @PostMapping("/{uuid}/revoke")
    @PreAuthorize(MANAGE_CERTIFICATE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<String>> revokeCertificate(
            @PathVariable UUID uuid,
            @RequestParam String reason) {
        certificateService.revokeCertificate(uuid, reason);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success("Certificate revoked successfully", "Certificate has been revoked: " + reason));
    }

    @Operation(
            summary = "Generate certificate URL",
            description = "Generates and updates the downloadable URL for a certificate."
    )
    @PostMapping("/{uuid}/generate-url")
    @PreAuthorize(MANAGE_CERTIFICATE)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> generateCertificateUrl(
            @PathVariable UUID uuid,
            @RequestParam String certificateUrl) {
        CertificateDTO updatedCertificate = certificateService.generateCertificateUrl(uuid, certificateUrl);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCertificate, "Certificate URL generated successfully"));
    }

    // ===== STUDENT CERTIFICATES =====

    @Operation(
            summary = "Get student certificates",
            description = "Retrieves all certificates earned by a specific student."
    )
    @GetMapping("/student/{studentUuid}")
    @PreAuthorize(READ_STUDENT_CERTIFICATES)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getStudentCertificates(
            @PathVariable UUID studentUuid) {
        List<CertificateDTO> certificates = certificateService.getCertificatesByStudent(studentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(certificates, "Student certificates retrieved successfully"));
    }

    @Operation(
            summary = "Get downloadable certificates",
            description = "Retrieves all valid certificates available for download by a student."
    )
    @GetMapping("/student/{studentUuid}/downloadable")
    @PreAuthorize(READ_STUDENT_CERTIFICATES)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getDownloadableCertificates(
            @PathVariable UUID studentUuid) {
        List<CertificateDTO> downloadableCertificates = certificateService.getDownloadableCertificates(studentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(downloadableCertificates, "Downloadable certificates retrieved successfully"));
    }

    // ===== CERTIFICATE ANALYTICS =====
    // Each of these returns every matching certificate on the platform, grades included, with no
    // course or organisation to scope it to. They are administrative reports and are guarded as such.

    @Operation(
            summary = "Get course certificates",
            description = "Retrieves all certificates issued for course completions. Platform administrators only."
    )
    @GetMapping("/course-certificates")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getCourseCertificates() {
        List<CertificateDTO> courseCertificates = certificateService.getCourseCertificates();
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courseCertificates, "Course certificates retrieved successfully"));
    }

    @Operation(
            summary = "Get program certificates",
            description = "Retrieves all certificates issued for program completions. Platform administrators only."
    )
    @GetMapping("/program-certificates")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getProgramCertificates() {
        List<CertificateDTO> programCertificates = certificateService.getProgramCertificates();
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(programCertificates, "Program certificates retrieved successfully"));
    }

    @Operation(
            summary = "Get revoked certificates",
            description = "Retrieves all revoked certificates for administrative review."
    )
    @GetMapping("/revoked")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getRevokedCertificates() {
        List<CertificateDTO> revokedCertificates = certificateService.getRevokedCertificates();
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(revokedCertificates, "Revoked certificates retrieved successfully"));
    }

    // ===== CERTIFICATE TEMPLATES =====

    @Operation(
            summary = "Create certificate template",
            description = "Creates a new certificate template for generating certificates."
    )
    @PostMapping("/templates")
    @PreAuthorize(TEACHING_STAFF)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateTemplateDTO>> createCertificateTemplate(
            @Valid @RequestBody CertificateTemplateDTO templateDTO) {
        CertificateTemplateDTO createdTemplate = certificateTemplateService.createCertificateTemplate(templateDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdTemplate, "Certificate template created successfully"));
    }

    @Operation(
            summary = "Get certificate templates",
            description = "Retrieves all available certificate templates."
    )
    @GetMapping("/templates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateTemplateDTO>>> getCertificateTemplates(
            Pageable pageable) {
        Page<CertificateTemplateDTO> templates = certificateTemplateService.getAllCertificateTemplates(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(templates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Certificate templates retrieved successfully"));
    }

    @Operation(
            summary = "Update certificate template",
            description = "Updates an existing certificate template. Platform administrators only - templates carry "
                    + "no owner, so editing one edits it for every certificate that renders from it."
    )
    @PutMapping("/templates/{templateUuid}")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateTemplateDTO>> updateCertificateTemplate(
            @PathVariable UUID templateUuid,
            @Valid @RequestBody CertificateTemplateDTO templateDTO) {
        CertificateTemplateDTO updatedTemplate = certificateTemplateService.updateCertificateTemplate(templateUuid, templateDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedTemplate, "Certificate template updated successfully"));
    }

    @Operation(
            summary = "Delete certificate template",
            description = "Removes a certificate template. Platform administrators only."
    )
    @DeleteMapping("/templates/{templateUuid}")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<Void> deleteCertificateTemplate(@PathVariable UUID templateUuid) {
        certificateTemplateService.deleteCertificateTemplate(templateUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== SEARCH ENDPOINTS =====

    @Operation(
            summary = "Search certificates",
            description = """
                    Advanced certificate search with flexible criteria and operators.
                    
                    **Common Certificate Search Examples:**
                    - `studentUuid=uuid` - All certificates for specific student
                    - `courseUuid=uuid` - All certificates for specific course
                    - `programUuid=uuid` - All certificates for specific program
                    - `isValid=true` - Only valid certificates
                    - `isValid=false` - Only revoked certificates
                    - `finalGrade_gte=85` - Certificates with grade 85%+
                    - `issuedDate_gte=2024-01-01T00:00:00` - Certificates issued from 2024
                    - `certificateNumber_like=CERT-2024` - Certificates from 2024
                    
                    **Certificate Analytics Queries:**
                    - `courseUuid_noteq=null&isValid=true` - Valid course certificates
                    - `programUuid_noteq=null&isValid=true` - Valid program certificates
                    - `finalGrade_between=80,100&isValid=true` - High-grade valid certificates

                    Platform administrators only: the criteria range over every certificate on the platform,
                    so there is no course or learner to scope the query to. Course staff list a learner's
                    certificates through `/student/{studentUuid}`.
                    """
    )
    @GetMapping("/search")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateDTO>>> searchCertificates(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CertificateDTO> certificates = certificateService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(certificates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Certificate search completed successfully"));
    }

    @Operation(
            summary = "Search certificate templates",
            description = """
                    Search certificate templates with filtering.
                    
                    **Common Template Search Examples:**
                    - `templateType=COURSE` - Course certificate templates
                    - `templateType=PROGRAM` - Program certificate templates
                    - `status=PUBLISHED` - Published templates
                    - `active=true` - Active templates
                    - `name_like=modern` - Templates with "modern" in name
                    """
    )
    @GetMapping("/templates/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateTemplateDTO>>> searchCertificateTemplates(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<CertificateTemplateDTO> templates = certificateTemplateService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(templates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Template search completed successfully"));
    }

}
