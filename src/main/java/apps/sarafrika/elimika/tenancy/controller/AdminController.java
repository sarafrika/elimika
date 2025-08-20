package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.AdminService;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing system administrators.
 * 
 * This controller handles operations related to system administrators who have
 * platform-wide privileges. All endpoints require system admin permissions.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "System administrator management operations")
public class AdminController {
    
    private final AdminService adminService;
    private final InvitationService invitationService;


    @GetMapping
    @Operation(
        summary = "Get all system administrators",
        description = "Retrieves a paginated list of all system administrators"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System administrators retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin privileges required")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Page<UserDTO>> getAllAdmins(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("Request to get all system administrators with pagination: {}", pageable);
        
        Page<UserDTO> admins = adminService.getAllAdmins(pageable);
        
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/check/{userUuid}")
    @Operation(
        summary = "Check if user has system admin privileges",
        description = "Verifies whether a specific user has system administrator privileges"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin privileges required")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Boolean>> isSystemAdmin(
            @Parameter(description = "UUID of the user to check", required = true)
            @PathVariable UUID userUuid) {
        
        log.debug("Request to check admin status for user {}", userUuid);
        
        boolean isAdmin = adminService.isSystemAdmin(userUuid);
        
        return ResponseEntity.ok(Map.of("isSystemAdmin", isAdmin));
    }

    @DeleteMapping("/revoke/{userUuid}")
    @Operation(
        summary = "Revoke admin privileges from a user",
        description = "Removes system administrator privileges from a user while preserving other domain assignments"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin privileges revoked successfully"),
        @ApiResponse(responseCode = "400", description = "User is not a system administrator"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin privileges required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UserDTO> revokeAdminPrivileges(
            @Parameter(description = "UUID of the admin user to demote", required = true)
            @PathVariable UUID userUuid) {
        
        log.info("Request to revoke admin privileges for user {}", userUuid);
        
        UserDTO demotedUser = adminService.revokeAdminPrivileges(userUuid);
        
        return ResponseEntity.ok(demotedUser);
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get system-wide statistics",
        description = "Retrieves comprehensive system statistics available only to system administrators"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin privileges required")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        
        log.debug("Request for system-wide statistics");
        
        Map<String, Object> stats = adminService.getSystemStatistics();
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @Operation(
        summary = "Get all users system-wide",
        description = "Retrieves a paginated list of all users across all organizations (admin-only function)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin privileges required")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Page<UserDTO>> getAllUsersSystemWide(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("Request to get all users system-wide with pagination: {}", pageable);
        
        Page<UserDTO> users = adminService.getAllUsersSystemWide(pageable);
        
        return ResponseEntity.ok(users);
    }

    // ================================
    // ADMIN INVITATION ENDPOINTS
    // ================================

    @PostMapping("/invite")
    @Operation(
        summary = "Send system administrator invitation",
        description = "Sends an email invitation to a user to become a system administrator. " +
                "The user must already have an account on the platform. Only existing system " +
                "administrators can send admin invitations."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Admin invitation sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - user doesn't exist or is already an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - admin privileges required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Pending invitation already exists for this user")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<InvitationDTO>> sendAdminInvitation(
            @Parameter(description = "Email address of the user to invite as system administrator", required = true)
            @RequestParam String recipientEmail,
            
            @Parameter(description = "Full name of the user to invite", required = true)
            @RequestParam String recipientName,
            
            @Parameter(description = "UUID of the admin sending the invitation", required = true)
            @RequestParam UUID inviterUuid,
            
            @Parameter(description = "Optional notes to include with the invitation")
            @RequestParam(required = false) String notes) {
        
        log.info("Request to send system administrator invitation to {} by admin {}", recipientEmail, inviterUuid);
        
        InvitationDTO invitation = invitationService.createAdminInvitation(
                recipientEmail, recipientName, inviterUuid, notes);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(invitation, "System administrator invitation sent successfully"));
    }

    @PostMapping("/invite/{userUuid}")
    @Operation(
        summary = "Send admin invitation by user UUID",
        description = "Sends an admin invitation to an existing user identified by their UUID. " +
                "This is a convenience endpoint that looks up the user's email and name automatically."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Admin invitation sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - user is already an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - admin privileges required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Pending invitation already exists for this user")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<InvitationDTO>> sendAdminInvitationByUuid(
            @Parameter(description = "UUID of the user to invite as system administrator", required = true)
            @PathVariable UUID userUuid,
            
            @Parameter(description = "UUID of the admin sending the invitation", required = true)
            @RequestParam UUID inviterUuid,
            
            @Parameter(description = "Optional notes to include with the invitation")
            @RequestParam(required = false) String notes) {
        
        log.info("Request to send system administrator invitation to user {} by admin {}", userUuid, inviterUuid);
        
        // Get user details for the invitation
        // This would need a method in AdminService to get user by UUID and extract email/name
        // For now, we'll throw an exception indicating this needs to be implemented
        throw new UnsupportedOperationException("This endpoint requires implementation of user lookup by UUID in AdminService");
    }

    @GetMapping("/invitations")
    @Operation(
        summary = "Get all admin invitations",
        description = "Retrieves all system administrator invitations regardless of status. " +
                "This includes pending, accepted, declined, expired, and cancelled invitations."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin invitations retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - admin privileges required")
    })
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<String>> getAllAdminInvitations() {
        
        log.debug("Request to get all system administrator invitations");
        
        // This would need a method in InvitationService to get admin invitations specifically
        // For now, we'll indicate this needs implementation
        return ResponseEntity.ok(ApiResponse.success("Implementation pending", "Admin invitation retrieval needs to be implemented"));
    }
}