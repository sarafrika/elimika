package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.student.dto.GuardianInvitationRegistrationRequestDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.MyStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.dto.PublicStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.service.StudentGuardianProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The guardian's side of a student's guardian details: the only route by which a parent gains
 * access. Accepting links the guardian to the student and grants {@code parent} without approval.
 */
@RestController
@RequestMapping(StudentGuardianInvitationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Student Guardian Invitations", description = "Accept or decline being a student's guardian")
public class StudentGuardianInvitationController {

    public static final String API_ROOT_PATH = "/api/v1/student-guardian-invitations";

    private final StudentGuardianProvisioningService provisioningService;
    private final DomainSecurityService domainSecurityService;

    @GetMapping("/token/{token}")
    @Operation(summary = "Read a guardian invitation from its link",
            description = "Public. has_account tells the UI whether to ask the guardian to sign in or to register.")
    public ResponseEntity<ApiResponse<PublicStudentGuardianInvitationDTO>> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(provisioningService.lookupByToken(token), "Invitation retrieved"));
    }

    @PostMapping("/token/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Accept a guardian invitation",
            description = "The signed-in account's email must be the invited one. Links the guardian and grants the parent domain.")
    public ResponseEntity<ApiResponse<GuardianStudentLinkDTO>> acceptByToken(@PathVariable String token) {
        GuardianStudentLinkDTO link = provisioningService.acceptByToken(token, requireCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(link, "You are now linked as a guardian"));
    }

    @PostMapping("/token/{token}/register")
    @Operation(summary = "Create an account from a guardian invitation and accept it",
            description = "Public. Creates the account for the invited email (a set-password email follows), then links "
                    + "the guardian. 409 when the email already has an account: sign in and accept instead.")
    public ResponseEntity<ApiResponse<GuardianStudentLinkDTO>> registerAndAccept(
            @PathVariable String token, @Valid @RequestBody GuardianInvitationRegistrationRequestDTO request) {
        GuardianStudentLinkDTO link = provisioningService.registerAndAccept(token, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(link, "Account created; check your email to set a password"));
    }

    @PostMapping("/token/{token}/decline")
    @Operation(summary = "Decline a guardian invitation", description = "Public; the link stops working.")
    public ResponseEntity<ApiResponse<Void>> declineByToken(@PathVariable String token) {
        provisioningService.declineByToken(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List open guardian invitations sent to my email")
    public ResponseEntity<ApiResponse<List<MyStudentGuardianInvitationDTO>>> getMine() {
        return ResponseEntity.ok(ApiResponse.success(
                provisioningService.listOpenInvitationsFor(requireCurrentUser()), "Invitations retrieved"));
    }

    @PostMapping("/{invitationUuid}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Accept a guardian invitation sent to my email")
    public ResponseEntity<ApiResponse<GuardianStudentLinkDTO>> acceptByUuid(@PathVariable UUID invitationUuid) {
        GuardianStudentLinkDTO link = provisioningService.acceptByUuid(invitationUuid, requireCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(link, "You are now linked as a guardian"));
    }

    @PostMapping("/{invitationUuid}/decline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Decline a guardian invitation sent to my email")
    public ResponseEntity<ApiResponse<Void>> declineByUuid(@PathVariable UUID invitationUuid) {
        provisioningService.declineByUuid(invitationUuid, requireCurrentUser());
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }

    private UUID requireCurrentUser() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new IllegalStateException("Authenticated user required for this action");
        }
        return userUuid;
    }
}
