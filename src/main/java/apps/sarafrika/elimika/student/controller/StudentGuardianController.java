package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.dto.StudentGuardianDTO;
import apps.sarafrika.elimika.student.service.StudentGuardianProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A student's guardians as named in onboarding, with whether each is linked or still invited. */
@RestController
@RequestMapping(StudentController.API_ROOT_PATH + "/{studentUuid}/guardians")
@RequiredArgsConstructor
@Tag(name = "Student Guardians", description = "Guardians a student named and the status of their access")
public class StudentGuardianController {

    private final StudentGuardianProvisioningService provisioningService;
    private final DomainSecurityService domainSecurityService;

    @GetMapping
    @PreAuthorize("@studentDirectorySecurityService.canViewGuardianDetails(#studentUuid)")
    @Operation(summary = "List a student's guardians",
            description = "Each guardian with status linked, invited, expired, declined or revoked. Includes guardians "
                    + "linked another way (uuid null). Restricted to the learner, an active guardian, a manager of one "
                    + "of the learner's organisations, or a platform admin.")
    public ResponseEntity<ApiResponse<List<StudentGuardianDTO>>> getGuardians(@PathVariable UUID studentUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                provisioningService.getGuardians(studentUuid), "Guardians retrieved successfully"));
    }

    @PostMapping("/{guardianUuid}/resend-invitation")
    @PreAuthorize("@studentDirectorySecurityService.canManageGuardianLinksFor(#studentUuid)")
    @Operation(summary = "Resend a guardian invitation",
            description = "Issues a fresh link (the old one stops working) and restarts the expiry. Refused for a guardian "
                    + "who is already linked. Restricted to the learner, a manager of one of the learner's "
                    + "organisations, or a platform admin.")
    public ResponseEntity<ApiResponse<StudentGuardianDTO>> resendInvitation(@PathVariable UUID studentUuid,
                                                                            @PathVariable UUID guardianUuid) {
        StudentGuardianDTO guardian = provisioningService.resendInvitation(
                studentUuid, guardianUuid, domainSecurityService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(guardian, "Guardian invitation sent"));
    }
}
