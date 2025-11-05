package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminActivityEventDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDashboardStatsDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainAssignmentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.AdminService;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.spi.InstructorManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for admin management operations
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-12-01
 */
@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management API", description = "System administrator management operations including domain assignment, user management, and dashboard statistics")
public class AdminController {

    private final AdminService adminService;
    private final OrganisationService organisationService;
    private final InstructorManagementService instructorManagementService;

    // ================================
    // ADMIN DOMAIN MANAGEMENT
    // ================================

    @Operation(
            summary = "Assign admin domain to user",
            description = "Assigns admin domain privileges to a user. This grants the user administrative access " +
                    "either globally (system admin) or within specific organizational contexts. " +
                    "Only existing system administrators can perform this operation."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin domain assigned successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid domain or user already has admin privileges")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or domain not found")
    @PostMapping("/users/{uuid}/domains")
    public ResponseEntity<ApiResponse<UserDTO>> assignAdminDomain(
            @Parameter(description = "UUID of the user to assign admin domain to. User must exist in the system.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Admin domain assignment request containing domain type, reason, and effective date")
            @Valid @RequestBody AdminDomainAssignmentRequestDTO request) {

        log.info("Assigning admin domain {} to user {}", request.domainName(), uuid);
        UserDTO updatedUser = adminService.assignAdminDomain(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Admin domain assigned successfully"));
    }

    @Operation(
            summary = "Remove admin domain from user",
            description = "Removes admin domain privileges from a user. This revokes the user's administrative access " +
                    "for the specified domain type. Only system administrators can perform this operation."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin domain removed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User does not have the specified admin domain")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or domain not found")
    @DeleteMapping("/users/{uuid}/domains/{domain}")
    public ResponseEntity<ApiResponse<UserDTO>> removeAdminDomain(
            @Parameter(description = "UUID of the user to remove admin domain from",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Domain name to remove",
                    example = "admin", required = true,
                    schema = @Schema(allowableValues = {"admin", "organisation_user"}))
            @PathVariable String domain,
            @Parameter(description = "Reason for removing admin privileges")
            @RequestParam(required = false) String reason) {

        log.info("Removing admin domain {} from user {} for reason: {}", domain, uuid, reason);
        UserDTO updatedUser = adminService.removeAdminDomain(uuid, domain, reason);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Admin domain removed successfully"));
    }

    // ================================
    // ADMIN USER MANAGEMENT
    // ================================

    @Operation(
            summary = "Get all admin users",
            description = "Retrieves a paginated list of all users with administrative privileges. " +
                    "Includes both system administrators and organization administrators. " +
                    "Supports filtering by admin level, status, and other criteria."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @GetMapping("/users/admins")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getAdminUsers(
            @Parameter(
                    description = "Optional filters for admin user search",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> filters,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting admin users with filters: {}", filters);
        var adminUsers = adminService.getAdminUsers(filters, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(adminUsers, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Admin users retrieved successfully"
        ));
    }

    @Operation(
            summary = "Get system admin users",
            description = "Retrieves a paginated list of users with global system administrator privileges. " +
                    "These users have platform-wide administrative access."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "System admin users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @GetMapping("/users/system-admins")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getSystemAdminUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting system admin users");
        var systemAdmins = adminService.getSystemAdminUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(systemAdmins, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "System admin users retrieved successfully"
        ));
    }

    @Operation(
            summary = "Get organization admin users",
            description = "Retrieves a paginated list of users with organization administrator privileges. " +
                    "These users have administrative access within specific organizational contexts."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization admin users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @GetMapping("/users/organization-admins")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getOrganizationAdminUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting organization admin users");
        var orgAdmins = adminService.getOrganizationAdminUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(orgAdmins, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Organization admin users retrieved successfully"
        ));
    }

    @Operation(
            summary = "Get users eligible for admin promotion",
            description = "Retrieves a paginated list of users who can be promoted to administrator roles. " +
                    "Excludes users who already have administrative privileges. Supports search by name or email."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Eligible users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @GetMapping("/users/eligible")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getAdminEligibleUsers(
            @Parameter(description = "Optional search term to filter users by name or email")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting admin eligible users with search term: {}", search);
        var eligibleUsers = adminService.getAdminEligibleUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(eligibleUsers, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Eligible users retrieved successfully"
        ));
    }

    // ================================
    // ADMIN DASHBOARD AND STATISTICS
    // ================================

    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieves comprehensive statistics for the admin dashboard including user metrics, " +
                    "organization metrics, content metrics, system performance, and admin-specific data."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<ApiResponse<AdminDashboardStatsDTO>> getDashboardStatistics() {
        log.debug("Getting admin dashboard statistics");
        AdminDashboardStatsDTO stats = adminService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard statistics retrieved successfully"));
    }

    @Operation(
            summary = "Get admin dashboard activity feed",
            description = "Retrieves a paginated list of recent administrative actions captured by the request audit trail."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard activity retrieved successfully")
    @GetMapping("/dashboard/activity-feed")
    public ResponseEntity<ApiResponse<PagedDTO<AdminActivityEventDTO>>> getDashboardActivity(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting admin dashboard activity feed with pagination: {}", pageable);
        Page<AdminActivityEventDTO> activity = adminService.getDashboardActivity(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(activity, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Dashboard activity retrieved successfully"
        ));
    }

    // ================================
    // ADMIN USER VALIDATION
    // ================================

    @Operation(
            summary = "Check if user is admin",
            description = "Checks whether a specific user has any type of administrative privileges. " +
                    "Returns true if the user has either system admin or organization admin roles."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin status check completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/{uuid}/is-admin")
    public ResponseEntity<ApiResponse<Boolean>> isUserAdmin(
            @Parameter(description = "UUID of the user to check for admin privileges",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {

        log.debug("Checking admin status for user: {}", uuid);
        boolean isAdmin = adminService.isAdmin(uuid);
        return ResponseEntity.ok(ApiResponse.success(isAdmin,
                isAdmin ? "User has admin privileges" : "User does not have admin privileges"));
    }

    @Operation(
            summary = "Check if user is system admin",
            description = "Checks whether a specific user has system administrator privileges. " +
                    "System admins have platform-wide administrative access."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "System admin status check completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/{uuid}/is-system-admin")
    public ResponseEntity<ApiResponse<Boolean>> isUserSystemAdmin(
            @Parameter(description = "UUID of the user to check for system admin privileges",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {

        log.debug("Checking system admin status for user: {}", uuid);
        boolean isSystemAdmin = adminService.isSystemAdmin(uuid);
        return ResponseEntity.ok(ApiResponse.success(isSystemAdmin,
                isSystemAdmin ? "User is a system admin" : "User is not a system admin"));
    }

    // ================================
    // ORGANIZATION VERIFICATION MANAGEMENT
    // ================================

    @Operation(
            summary = "Get pending organization approvals",
            description = "Retrieves a paginated list of organizations that are awaiting admin verification. " +
                    "Results include organisations where the admin_verified flag is false or not yet set."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending organizations retrieved successfully")
    @GetMapping("/organizations/pending")
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationDTO>>> getPendingOrganisations(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Fetching pending organisations for approval with pagination: {}", pageable);
        Page<OrganisationDTO> pendingOrganisations = organisationService.getUnverifiedOrganisations(pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(pendingOrganisations, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Pending organizations retrieved successfully"
        ));
    }

    @Operation(
            summary = "Moderate organization verification",
            description = "Handles organization approval workflows using a single endpoint. " +
                    "Supports approving, rejecting, or revoking admin verification status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization moderation completed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid moderation action supplied")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    @PostMapping("/organizations/{uuid}/moderate")
    public ResponseEntity<ApiResponse<OrganisationDTO>> moderateOrganisation(
            @Parameter(description = "UUID of the organization to moderate. Must be an existing organization identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Moderation action to perform",
                    schema = @Schema(allowableValues = {"approve", "reject", "revoke"}), required = true)
            @RequestParam("action") String action,
            @Parameter(description = "Optional reason for the chosen moderation action")
            @RequestParam(required = false) String reason) {

        String normalizedAction = action.toLowerCase();
        log.info("Admin moderating organization {} with action '{}' and reason: {}", uuid, normalizedAction, reason);

        OrganisationDTO organisationDTO;
        String message;

        switch (normalizedAction) {
            case "approve" -> {
                organisationDTO = organisationService.verifyOrganisation(uuid, reason);
                message = "Organization approved successfully";
            }
            case "reject" -> {
                organisationDTO = organisationService.unverifyOrganisation(uuid, reason);
                message = "Organization rejected successfully";
            }
            case "revoke" -> {
                organisationDTO = organisationService.unverifyOrganisation(uuid, reason);
                message = "Organization verification revoked";
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported moderation action: " + action + ". Allowed values: approve, reject, revoke"
            );
        }

        return ResponseEntity.ok(ApiResponse.success(organisationDTO, message));
    }

    @Operation(
            summary = "Check if organization is verified",
            description = "Checks whether a specific organization has been verified by an admin. " +
                    "Returns true if the organization has admin verification status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification status check completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    @GetMapping("/organizations/{uuid}/verification-status")
    public ResponseEntity<ApiResponse<Boolean>> isOrganisationVerified(
            @Parameter(description = "UUID of the organization to check verification status for.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {

        log.debug("Checking verification status for organization: {}", uuid);
        boolean isVerified = organisationService.isOrganisationVerified(uuid);
        return ResponseEntity.ok(ApiResponse.success(isVerified,
                isVerified ? "Organization is verified" : "Organization is not verified"));
    }

    // ================================
    // INSTRUCTOR VERIFICATION MANAGEMENT
    // ================================

    @Operation(
            summary = "Verify an instructor",
            description = "Verifies/approves an instructor by setting the admin_verified flag to true. " +
                    "Only system administrators can perform this operation. Verified instructors gain access to " +
                    "additional platform features and display verification badges."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Instructor verified successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Instructor not found")
    @PostMapping("/instructors/{uuid}/verify")
    public ResponseEntity<ApiResponse<InstructorDTO>> verifyInstructor(
            @Parameter(description = "UUID of the instructor to verify. Must be an existing instructor identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Optional reason for verification")
            @RequestParam(required = false) String reason) {

        log.info("Admin verifying instructor {} for reason: {}", uuid, reason);
        InstructorDTO verified = instructorManagementService.verifyInstructor(uuid, reason);
        return ResponseEntity.ok(ApiResponse.success(verified, "Instructor verified successfully"));
    }

    @Operation(
            summary = "Remove verification from an instructor",
            description = "Removes verification from an instructor by setting the admin_verified flag to false. " +
                    "Only system administrators can perform this operation. This may revoke access to certain platform features."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Instructor verification removed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient privileges - system admin required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Instructor not found")
    @PostMapping("/instructors/{uuid}/unverify")
    public ResponseEntity<ApiResponse<InstructorDTO>> unverifyInstructor(
            @Parameter(description = "UUID of the instructor to remove verification from. Must be an existing instructor identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Optional reason for removing verification")
            @RequestParam(required = false) String reason) {

        log.info("Admin removing verification from instructor {} for reason: {}", uuid, reason);
        InstructorDTO unverified = instructorManagementService.unverifyInstructor(uuid, reason);
        return ResponseEntity.ok(ApiResponse.success(unverified, "Instructor verification removed successfully"));
    }

    @Operation(
            summary = "Check if instructor is verified",
            description = "Checks whether a specific instructor has been verified by an admin. " +
                    "Returns true if the instructor has admin verification status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification status check completed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Instructor not found")
    @GetMapping("/instructors/{uuid}/verification-status")
    public ResponseEntity<ApiResponse<Boolean>> isInstructorVerified(
            @Parameter(description = "UUID of the instructor to check verification status for.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {

        log.debug("Checking verification status for instructor: {}", uuid);
        boolean isVerified = instructorManagementService.isInstructorVerified(uuid);
        return ResponseEntity.ok(ApiResponse.success(isVerified,
                isVerified ? "Instructor is verified" : "Instructor is not verified"));
    }
}
