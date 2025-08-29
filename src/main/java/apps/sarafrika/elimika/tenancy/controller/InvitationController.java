package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.InvitationPreviewDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/invitations")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Invitations API", description = "Token-based invitation operations for email acceptance/decline flows, public invitation validation, and React frontend integration")
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
    // REACT FRONTEND INTEGRATION ENDPOINTS
    // ================================

    @Operation(
            summary = "Preview invitation details (PUBLIC)",
            description = """
                    Gets public-safe invitation details by token without requiring authentication.
                    Used by React frontend to display invitation information before user login/registration.
                    
                    **URL Structure:** https://elimika.sarafrika.com/invitations/accept?token={token}
                    
                    **Response includes:**
                    - Recipient name and organization details
                    - Role being offered with description  
                    - Inviter information and personal notes
                    - Expiration status and registration requirements
                    
                    **Security:** Token-based validation ensures only valid invitations are previewed.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation preview retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or malformed token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found or token expired")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<InvitationPreviewDTO>> previewInvitation(
            @Parameter(
                    description = "Invitation token from email link. Must be a valid, non-expired invitation token.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd",
                    required = true
            )
            @RequestParam("token") @NotBlank String token) {

        log.debug("Processing invitation preview request for token: {}", token);
        
        InvitationPreviewDTO preview = invitationService.previewInvitation(token);
        return ResponseEntity.ok(ApiResponse.success(preview, "Invitation preview retrieved successfully"));
    }

    @Operation(
            summary = "Accept invitation (AUTHENTICATED)",
            description = """
                    Accepts an invitation for a Keycloak-authenticated user.
                    Used by React frontend after user authentication to process invitation acceptance.
                    
                    **Flow:**
                    1. User authenticates via Keycloak (login or registration)
                    2. React frontend calls this endpoint with invitation token
                    3. System validates token and user email match
                    4. Creates user-organization relationship and assigns role
                    5. Sends confirmation email to user
                    
                    **Authentication:** JWT token from Keycloak required in Authorization header.
                    **Email Validation:** User's email from JWT must match invitation recipient.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation accepted successfully, user added to organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid token, email mismatch, or invitation already processed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required - user must be logged in via Keycloak")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found or user not found in database")
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<UserDTO>> acceptInvitation(
            @Parameter(
                    description = "Invitation token from email link. Must match an active, non-expired invitation.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd",
                    required = true
            )
            @RequestParam("token") @NotBlank String token) {

        String userEmail = getAuthenticatedUserEmail();
        log.info("Processing invitation acceptance for token: {} by user: {}", token, userEmail);
        
        UserDTO updatedUser = invitationService.acceptInvitationAuthenticated(token, userEmail);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Invitation accepted successfully"));
    }

    @Operation(
            summary = "Decline invitation (AUTHENTICATED)", 
            description = """
                    Declines an invitation for a Keycloak-authenticated user.
                    Used by React frontend after user authentication to process invitation decline.
                    
                    **Flow:**
                    1. User authenticates via Keycloak
                    2. React frontend calls this endpoint with invitation token  
                    3. System validates token and user email match
                    4. Marks invitation as declined with timestamp
                    5. Sends decline notification email to inviter
                    
                    **Authentication:** JWT token from Keycloak required in Authorization header.
                    **Email Validation:** User's email from JWT must match invitation recipient.
                    **Note:** Declined invitations cannot be reactivated.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation declined successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid token, email mismatch, or invitation already processed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required - user must be logged in via Keycloak")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found or user not found in database")
    @PostMapping("/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(
            @Parameter(
                    description = "Invitation token from email link. Must match an active, non-expired invitation.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd",
                    required = true
            )
            @RequestParam("token") @NotBlank String token) {

        String userEmail = getAuthenticatedUserEmail();
        log.info("Processing invitation decline for token: {} by user: {}", token, userEmail);
        
        invitationService.declineInvitationAuthenticated(token, userEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined successfully"));
    }

    @Operation(
            summary = "Process pending invitations for authenticated user",
            description = """
                    Automatically processes all pending invitations for a newly authenticated user.
                    Typically called after successful Keycloak registration/login to handle any outstanding invitations.
                    
                    **Use Cases:**
                    - New user registers and has pending invitations
                    - Existing user logs in and has new invitations waiting
                    - Bulk processing of invitations after authentication
                    
                    **Behavior:**
                    - Only processes valid, non-expired invitations
                    - Automatically accepts all matching invitations for user's email
                    - Sends confirmation emails for each accepted invitation
                    - Returns list of successfully processed invitations
                    
                    **Authentication:** JWT token from Keycloak required in Authorization header.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending invitations processed successfully (may be empty list)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required - user must be logged in via Keycloak")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found in database after authentication")
    @PostMapping("/process-pending")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> processPendingInvitations() {

        String userEmail = getAuthenticatedUserEmail();
        log.info("Processing pending invitations for user: {}", userEmail);
        
        List<InvitationDTO> acceptedInvitations = invitationService.processPendingInvitationsForUser(userEmail);
        String message = acceptedInvitations.isEmpty() 
                ? "No pending invitations found" 
                : String.format("Successfully processed %d pending invitations", acceptedInvitations.size());
                
        return ResponseEntity.ok(ApiResponse.success(acceptedInvitations, message));
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

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Extracts the authenticated user's email from Keycloak JWT token.
     * 
     * @return the user's email address from JWT claims
     * @throws IllegalStateException if user is not authenticated or email not found in token
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        
        // Extract email from JWT token (Keycloak provides email in JWT claims)
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalStateException("Email not found in authentication token");
            }
            return email.trim().toLowerCase();
        }
        
        throw new IllegalStateException("Invalid authentication type - JWT expected");
    }
}