package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.dto.GuardianStudentDashboardDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkRequest;
import apps.sarafrika.elimika.student.dto.GuardianStudentSummaryDTO;
import apps.sarafrika.elimika.student.service.GuardianAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guardians")
@RequiredArgsConstructor
@Tag(name = "Guardian Access", description = "Endpoints for linking guardians/parents to learner dashboards.")
public class GuardianAccessController {

    private final GuardianAccessService guardianAccessService;
    private final DomainSecurityService domainSecurityService;

    @PostMapping("/links")
    @PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
    @Operation(
            summary = "Link a guardian to a learner",
            description = "Grants a guardian/parent access to monitor a learner using their own credentials.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Guardian link created",
                            content = @Content(schema = @Schema(implementation = GuardianStudentLinkDTO.class)))
            }
    )
    public ResponseEntity<ApiResponse<GuardianStudentLinkDTO>> createLink(
            @Valid @RequestBody GuardianStudentLinkRequest request) {
        UUID actorUuid = requireCurrentUser();
        GuardianStudentLinkDTO dto = guardianAccessService.createOrUpdateLink(request, actorUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Guardian linked to student successfully"));
    }

    @DeleteMapping("/links/{linkUuid}")
    @PreAuthorize("@domainSecurityService.isInstructorOrAdmin()")
    @Operation(summary = "Revoke guardian access",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Guardian access revoked")
            })
    public ResponseEntity<ApiResponse<Void>> revokeLink(@PathVariable UUID linkUuid,
                                                        @RequestParam(required = false) String reason) {
        guardianAccessService.revokeLink(linkUuid, requireCurrentUser(), reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Guardian access revoked"));
    }

    @GetMapping("/me/students")
    @PreAuthorize("@domainSecurityService.isGuardian()")
    @Operation(summary = "List guardian-linked students")
    public ResponseEntity<ApiResponse<List<GuardianStudentSummaryDTO>>> getMyStudents() {
        UUID guardianUuid = requireCurrentUser();
        List<GuardianStudentSummaryDTO> students = guardianAccessService.getGuardianStudentSummaries(guardianUuid);
        return ResponseEntity.ok(ApiResponse.success(students, "Guardian students retrieved"));
    }

    @GetMapping("/students/{studentUuid}/dashboard")
    @PreAuthorize("@guardianLinkSecurityService.canAccessStudent(#studentUuid)")
    @Operation(summary = "Fetch learner dashboard for guardian access",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard data ready",
                            content = @Content(schema = @Schema(implementation = GuardianStudentDashboardDTO.class)))
            })
    public ResponseEntity<ApiResponse<GuardianStudentDashboardDTO>> getStudentDashboard(
            @PathVariable UUID studentUuid) {
        UUID guardianUuid = requireCurrentUser();
        GuardianStudentDashboardDTO dashboard = guardianAccessService.getGuardianDashboard(guardianUuid, studentUuid);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Guardian dashboard ready"));
    }

    private UUID requireCurrentUser() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new IllegalStateException("Authenticated user required for this action");
        }
        return userUuid;
    }
}
