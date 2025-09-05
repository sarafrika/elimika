package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for comprehensive certificate management and verification.
 */
@RestController
@RequestMapping(CertificateController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Certificate Management", description = "Complete certificate lifecycle including generation, verification, and templates")
public class CertificateController {

    public static final String API_ROOT_PATH = "/api/v1/certificates";

    private final CertificateService certificateService;
    private final CertificateTemplateService certificateTemplateService;

    // ===== CERTIFICATE BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new certificate",
            description = "Manually creates a certificate record with automatic number generation.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Certificate created successfully",
                            content = @Content(schema = @Schema(implementation = CertificateDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> createCertificate(
            @Valid @RequestBody CertificateDTO certificateDTO) {
        CertificateDTO createdCertificate = certificateService.createCertificate(certificateDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdCertificate, "Certificate created successfully"));
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
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> getCertificateByUuid(
            @PathVariable UUID uuid) {
        CertificateDTO certificateDTO = certificateService.getCertificateByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(certificateDTO, "Certificate retrieved successfully"));
    }

    @Operation(
            summary = "Get all certificates",
            description = "Retrieves paginated list of all certificates with filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateDTO>>> getAllCertificates(
            Pageable pageable) {
        Page<CertificateDTO> certificates = certificateService.getAllCertificates(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(certificates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Certificates retrieved successfully"));
    }

    @Operation(
            summary = "Update certificate",
            description = "Updates an existing certificate with selective field updates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Certificate updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateDTO>> updateCertificate(
            @PathVariable UUID uuid,
            @Valid @RequestBody CertificateDTO certificateDTO) {
        CertificateDTO updatedCertificate = certificateService.updateCertificate(uuid, certificateDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedCertificate, "Certificate updated successfully"));
    }

    @Operation(
            summary = "Delete certificate",
            description = "Permanently removes a certificate record.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Certificate deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Certificate not found")
            }
    )
    @DeleteMapping("/{uuid}")
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

    @Operation(
            summary = "Verify certificate",
            description = "Verifies the authenticity of a certificate using its certificate number.",
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
            description = "Retrieves certificate details using certificate number for public verification."
    )
    @GetMapping("/number/{certificateNumber}")
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
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getDownloadableCertificates(
            @PathVariable UUID studentUuid) {
        List<CertificateDTO> downloadableCertificates = certificateService.getDownloadableCertificates(studentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(downloadableCertificates, "Downloadable certificates retrieved successfully"));
    }

    // ===== CERTIFICATE ANALYTICS =====

    @Operation(
            summary = "Get course certificates",
            description = "Retrieves all certificates issued for course completions."
    )
    @GetMapping("/course-certificates")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CertificateDTO>>> getCourseCertificates() {
        List<CertificateDTO> courseCertificates = certificateService.getCourseCertificates();
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courseCertificates, "Course certificates retrieved successfully"));
    }

    @Operation(
            summary = "Get program certificates",
            description = "Retrieves all certificates issued for program completions."
    )
    @GetMapping("/program-certificates")
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
            description = "Updates an existing certificate template."
    )
    @PutMapping("/templates/{templateUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CertificateTemplateDTO>> updateCertificateTemplate(
            @PathVariable UUID templateUuid,
            @Valid @RequestBody CertificateTemplateDTO templateDTO) {
        CertificateTemplateDTO updatedTemplate = certificateTemplateService.updateCertificateTemplate(templateUuid, templateDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedTemplate, "Certificate template updated successfully"));
    }

    @Operation(
            summary = "Delete certificate template",
            description = "Removes a certificate template."
    )
    @DeleteMapping("/templates/{templateUuid}")
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
                    """
    )
    @GetMapping("/search")
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