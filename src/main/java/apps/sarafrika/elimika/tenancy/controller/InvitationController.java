package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations API", description = "Token-based invitation operations for email acceptance/decline flows and public invitation validation")
class InvitationController {
    private final InvitationService invitationService;

    @Operation(
            summary = "Get invitation details by token",
            description = "Retrieves complete invitation information using the unique token from the invitation email. " +
                    "This endpoint is typically used by the invitation acceptance/decline pages to display invitation details " +
                    "before the user makes their decision. Includes organization, branch, and role information."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation details retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation token not found or invalid")
    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<InvitationDTO>> getInvitationByToken(
            @Parameter(description = "Unique invitation token from the invitation email URL. This is the 64-character identifier for the specific invitation.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd", required = true)
            @PathVariable String token) {
        InvitationDTO invitation = invitationService.getInvitationByToken(token);
        return ResponseEntity.ok(ApiResponse.success(invitation, "Invitation retrieved successfully"));
    }

    @Operation(
            summary = "Validate invitation token",
            description = "Validates whether an invitation token is currently valid and can be accepted or declined. " +
                    "Checks if the invitation exists, is in PENDING status, and has not expired. " +
                    "This endpoint is useful for pre-validation before displaying acceptance/decline forms."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Validation completed - check response body for result")
    @GetMapping("/validate/{token}")
    public ResponseEntity<ApiResponse<Boolean>> validateInvitation(
            @Parameter(description = "Unique invitation token to validate. This is the 64-character token from invitation emails.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd", required = true)
            @PathVariable String token) {
        boolean valid = invitationService.isInvitationValid(token);
        return ResponseEntity.ok(ApiResponse.success(valid, "Validation completed"));
    }

    @Operation(
            summary = "Get pending invitations for email address",
            description = "Retrieves all pending invitations sent to a specific email address across all organizations and branches. " +
                    "This endpoint helps users see all outstanding invitations they have received. " +
                    "Only returns invitations with PENDING status that haven't expired."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending invitations retrieved successfully (may be empty list)")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getPendingInvitationsForEmail(
            @Parameter(description = "Email address to search for pending invitations. Must be a valid email format. Search is case-insensitive.",
                    example = "john.doe@example.com", required = true)
            @RequestParam String email) {
        List<InvitationDTO> invitations = invitationService.getPendingInvitationsForEmail(email);
        return ResponseEntity.ok(ApiResponse.success(invitations, "Pending invitations retrieved successfully"));
    }

    // ================================
    // MAINTENANCE ENDPOINTS (for system administration)
    // ================================

    @Operation(
            summary = "Mark expired invitations",
            description = "System maintenance endpoint to mark all expired pending invitations as expired. " +
                    "This is typically called by scheduled jobs to clean up expired invitations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expired invitations marked successfully")
    @PostMapping("/maintenance/mark-expired")
    public ResponseEntity<ApiResponse<Integer>> markExpiredInvitations() {
        int markedCount = invitationService.markExpiredInvitations();
        return ResponseEntity.ok(ApiResponse.success(markedCount, "Expired invitations marked successfully"));
    }

    @Operation(
            summary = "Send expiry reminders",
            description = "System maintenance endpoint to send reminder emails for invitations expiring soon. " +
                    "This is typically called by scheduled jobs to notify recipients about expiring invitations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Expiry reminders sent successfully")
    @PostMapping("/maintenance/send-reminders")
    public ResponseEntity<ApiResponse<Integer>> sendExpiryReminders(
            @Parameter(description = "Number of hours before expiry to send reminder. Default is 24 hours.",
                    example = "24")
            @RequestParam(defaultValue = "24") int hoursBeforeExpiry) {
        int remindersSent = invitationService.sendExpiryReminders(hoursBeforeExpiry);
        return ResponseEntity.ok(ApiResponse.success(remindersSent, "Expiry reminders sent successfully"));
    }

    @Operation(
            summary = "Cleanup old invitations",
            description = "System maintenance endpoint to delete old invitations that are expired, declined, or cancelled. " +
                    "This helps maintain database cleanliness by removing old invitation records."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Old invitations cleaned up successfully")
    @DeleteMapping("/maintenance/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldInvitations(
            @Parameter(description = "Delete invitations older than this many days. Default is 90 days.",
                    example = "90")
            @RequestParam(defaultValue = "90") int daysOld) {
        int deletedCount = invitationService.cleanupOldInvitations(daysOld);
        return ResponseEntity.ok(ApiResponse.success(deletedCount, "Old invitations cleaned up successfully"));
    }
}