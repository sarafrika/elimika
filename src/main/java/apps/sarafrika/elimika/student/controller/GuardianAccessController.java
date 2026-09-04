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

    /**
     * Creating a link grants the named user the {@code parent} domain and standing access to this
     * learner's record, so it is gated on custody of the learner — the learner themselves, a manager
     * of an organisation they belong to, or a platform admin — rather than on holding the
     * {@code instructor} or {@code admin} domain somewhere. A domain a user can hold platform-wide
     * says nothing about which children they may appoint guardians for, and the previous role check
     * let any instructor grant anyone, themselves included, guardian access to any learner.
     */
    @PostMapping("/links")
    @PreAuthorize("@studentDirectorySecurityService.canManageGuardianLinksFor(#request.studentUuid())")
    @Operation(
            summary = "Link a guardian to a learner",
            description = "Grants a guardian/parent access to monitor a learner using their own credentials. "
                    + "Restricted to the learner, a manager of one of the learner's organisations, or a platform admin.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Guardian link created",
                            content = @Content(schema = @Schema(implementation = GuardianStudentLinkDTO.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller has no custody of that learner")
            }
    )
    public ResponseEntity<ApiResponse<GuardianStudentLinkDTO>> createLink(
            @Valid @RequestBody GuardianStudentLinkRequest request) {
        UUID actorUuid = requireCurrentUser();
        GuardianStudentLinkDTO dto = guardianAccessService.createOrUpdateLink(request, actorUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Guardian linked to student successfully"));
    }

    /**
     * Revoking is the same custody test as creating, widened by one party: the guardian on the link
     * may always give up their own access.
     */
    @DeleteMapping("/links/{linkUuid}")
    @PreAuthorize("@studentDirectorySecurityService.canRevokeGuardianLink(#linkUuid)")
    @Operation(summary = "Revoke guardian access",
            description = "Restricted to the learner, a manager of one of the learner's organisations, "
                    + "a platform admin, or the guardian giving up their own access.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Guardian access revoked"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller may not revoke that link")
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
