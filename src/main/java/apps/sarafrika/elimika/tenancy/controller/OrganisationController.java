package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/organisations")
@RequiredArgsConstructor
@Tag(name = "Organisations API", description = "Complete organization management including users, training branches, and invitation management within organizational hierarchy")
class OrganisationController {
    private final OrganisationService organisationService;
    private final UserService userService;
    private final InvitationService invitationService;
    private final TrainingBranchService trainingBranchService;

    // ================================
    // CORE ORGANISATION MANAGEMENT
    // ================================

    @Operation(summary = "Create a new organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organisation created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganisationDTO>> createOrganisation(
            @Valid @RequestBody OrganisationDTO organisationDTO) {
        OrganisationDTO created = organisationService.createOrganisation(organisationDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Organisation created successfully"));
    }

    @Operation(summary = "Get an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<OrganisationDTO>> getOrganisationByUuid(
            @Parameter(description = "UUID of the organisation to retrieve. Must be an existing organisation identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        OrganisationDTO organisation = organisationService.getOrganisationByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(organisation, "Organisation retrieved successfully"));
    }

    @Operation(summary = "Get all organisations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisations retrieved successfully")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationDTO>>> getAllOrganisations(Pageable pageable) {
        Page<OrganisationDTO> organisations = organisationService.getAllOrganisations(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(organisations, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Organisations retrieved successfully"));
    }

    @Operation(summary = "Update an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<OrganisationDTO>> updateOrganisation(
            @Parameter(description = "UUID of the organisation to update. Must be an existing organisation identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody OrganisationDTO organisationDTO) {
        OrganisationDTO updated = organisationService.updateOrganisation(uuid, organisationDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Organisation updated successfully"));
    }

    @Operation(summary = "Delete an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganisation(
            @Parameter(description = "UUID of the organisation to delete. This will soft-delete the organisation and all user relationships.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        organisationService.deleteOrganisation(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Organisation deleted successfully"));
    }

    @Operation(summary = "Search organisations",
            description = "Fetches a paginated list of organisations based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of organisations matching the search criteria")
    @GetMapping("search")
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationDTO>>> search(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam() Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(organisationService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Organisations search successful"));
    }

    // ================================
    // ORGANISATION USERS MANAGEMENT
    // ================================

    @Operation(summary = "Get users by organisation ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("/{uuid}/users")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getUsersByOrganisation(
            @Parameter(description = "UUID of the organisation to get users for. Must be an existing organisation identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            Pageable pageable) {
        Page<UserDTO> users = userService.getUsersByOrganisation(uuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(users, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users retrieved successfully"));
    }

    @Operation(
            summary = "Get users by organisation and domain",
            description = "Retrieves all users in the organisation filtered by their role/domain. " +
                    "This endpoint is useful for getting all instructors, students, or admins within an organisation."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid domain name")
    @GetMapping("/{uuid}/users/domain/{domainName}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByOrganisationAndDomain(
            @Parameter(description = "UUID of the organisation to get users for. Must be an existing organisation identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Domain name to filter users by. Valid values: 'student', 'instructor', 'admin', 'organisation_user'",
                    example = "instructor", required = true)
            @PathVariable String domainName) {
        List<UserDTO> users = userService.getUsersByOrganisationAndDomain(uuid, domainName);
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    // ================================
    // ORGANISATION INVITATIONS MANAGEMENT
    // ================================

    @Operation(
            summary = "Create organization invitation",
            description = "Creates and sends an email invitation for a user to join this specific organization with a defined role. " +
                    "If a training branch UUID is provided, the invitation will be branch-specific within the organization. " +
                    "The invitation email will be sent to the recipient with acceptance and decline links containing the unique token."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitation created and email sent successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data: duplicate invitation, invalid domain, or branch doesn't belong to organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization, inviter user, or training branch not found")
    @PostMapping("/{uuid}/invitations")
    public ResponseEntity<ApiResponse<InvitationDTO>> createOrganizationInvitation(
            @Parameter(description = "UUID of the organization the user is being invited to join. Must be an existing, active organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Email address of the person being invited. Must be a valid email format.",
                    example = "john.doe@example.com", required = true)
            @RequestParam("recipient_email") String recipientEmail,
            @Parameter(description = "Full name of the person being invited. Used in email templates and invitation records.",
                    example = "John Doe", required = true)
            @RequestParam("recipient_name") String recipientName,
            @Parameter(description = "Role/domain name being offered to the recipient. Valid values: 'student', 'instructor', 'admin', 'organisation_user'",
                    example = "instructor", required = true)
            @RequestParam("domain_name") String domainName,
            @Parameter(description = "Optional UUID of a training branch within the organization. If provided, the invitation will be branch-specific. Must belong to the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = false)
            @RequestParam(value = "branch_uuid", required = false) UUID branchUuid,
            @Parameter(description = "UUID of the user who is sending this invitation. Must be an existing user with appropriate permissions in the organization.",
                    example = "550e8400-e29b-41d4-a716-446655440004", required = true)
            @RequestParam("inviter_uuid") UUID inviterUuid,
            @Parameter(description = "Optional personal message or notes to include with the invitation email. Maximum 500 characters.",
                    example = "Welcome to our training program! We're excited to have you join our team.", required = false)
            @RequestParam(value = "notes", required = false) String notes) {
        InvitationDTO created = invitationService.createOrganisationInvitation(
                recipientEmail, recipientName, uuid, domainName, branchUuid, inviterUuid, notes);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Invitation created successfully"));
    }

    @Operation(
            summary = "Get all invitations for organization",
            description = "Retrieves all invitations (regardless of status) that have been sent for this specific organization. " +
                    "This includes organization-level invitations and branch-specific invitations within the organization. " +
                    "Results are ordered by creation date (most recent first) and include all invitation statuses."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization invitations retrieved successfully (may be empty list)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    @GetMapping("/{uuid}/invitations")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getOrganizationInvitations(
            @Parameter(description = "UUID of the organization to retrieve invitations for. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        List<InvitationDTO> invitations = invitationService.getOrganisationInvitations(uuid);
        return ResponseEntity.ok(ApiResponse.success(invitations, "Invitations retrieved successfully"));
    }

    @Operation(
            summary = "Cancel pending invitation",
            description = "Cancels a pending invitation within this organization, preventing it from being accepted or declined. " +
                    "Only the original inviter or an organization administrator can cancel invitations. " +
                    "This action is irreversible and the invitation cannot be reactivated."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation cancelled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invitation is not pending, or user lacks permission to cancel")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found")
    @DeleteMapping("/{uuid}/invitations/{invitationUuid}")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @Parameter(description = "UUID of the organization that owns the invitation. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the invitation to cancel. Must be a pending invitation that hasn't been accepted, declined, or expired.",
                    example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable UUID invitationUuid,
            @Parameter(description = "UUID of the user requesting to cancel the invitation. Must be either the original inviter or an administrator of the organization.",
                    example = "550e8400-e29b-41d4-a716-446655440004", required = true)
            @RequestParam("canceller_uuid") UUID cancellerUuid) {
        invitationService.cancelInvitation(invitationUuid, cancellerUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation cancelled successfully"));
    }

    @Operation(
            summary = "Resend invitation email",
            description = "Resends the invitation email to the recipient with a fresh expiration date. " +
                    "Only pending invitations can be resent. The invitation expiry date will be extended from the current time. " +
                    "Only the original inviter or an organization administrator can resend invitations."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation email resent successfully with updated expiry date")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invitation is not pending, or user lacks permission to resend")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation not found")
    @PostMapping("/{uuid}/invitations/{invitationUuid}/resend")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(
            @Parameter(description = "UUID of the organization that owns the invitation. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the invitation to resend. Must be a pending invitation that hasn't been accepted, declined, or cancelled.",
                    example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable UUID invitationUuid,
            @Parameter(description = "UUID of the user requesting to resend the invitation. Must be either the original inviter or an administrator of the organization.",
                    example = "550e8400-e29b-41d4-a716-446655440004", required = true)
            @RequestParam("resender_uuid") UUID resenderUuid) {
        invitationService.resendInvitation(invitationUuid, resenderUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation resent successfully"));
    }

    // ================================
    // TRAINING BRANCHES MANAGEMENT
    // ================================

    @Operation(summary = "Create a new training branch within organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training branch created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/{uuid}/training-branches")
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> createTrainingBranch(
            @Parameter(description = "UUID of the organization to create the training branch in. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO branchWithOrgUuid = new TrainingBranchDTO(
                trainingBranchDTO.uuid(),
                uuid,
                trainingBranchDTO.branchName(),
                trainingBranchDTO.address(),
                trainingBranchDTO.pocUserUuid(),
                trainingBranchDTO.active(),
                null, null
        );
        TrainingBranchDTO created = trainingBranchService.createTrainingBranch(branchWithOrgUuid);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Training branch created successfully"));
    }

    @Operation(summary = "Get training branches by organisation UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branches retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @GetMapping("/{uuid}/training-branches")
    public ResponseEntity<ApiResponse<PagedDTO<TrainingBranchDTO>>> getTrainingBranchesByOrganisation(
            @Parameter(description = "UUID of the organisation to get training branches for. Must be an existing organisation identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            Pageable pageable) {
        Page<TrainingBranchDTO> trainingBranches = trainingBranchService.getTrainingBranchesByOrganisation(uuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(trainingBranches, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Training branches retrieved successfully"));
    }

    @Operation(summary = "Get a training branch by UUID within organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @GetMapping("/{uuid}/training-branches/{branchUuid}")
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> getTrainingBranchByUuid(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to retrieve. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid) {
        TrainingBranchDTO trainingBranch = trainingBranchService.getTrainingBranchByUuid(branchUuid);
        return ResponseEntity.ok(ApiResponse.success(trainingBranch, "Training branch retrieved successfully"));
    }

    @Operation(summary = "Update a training branch by UUID within organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}/training-branches/{branchUuid}")
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> updateTrainingBranch(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to update. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO updated = trainingBranchService.updateTrainingBranch(branchUuid, trainingBranchDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Training branch updated successfully"));
    }

    @Operation(summary = "Delete a training branch by UUID within organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @DeleteMapping("/{uuid}/training-branches/{branchUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteTrainingBranch(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to delete. This will soft-delete the branch and remove all user assignments.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid) {
        trainingBranchService.deleteTrainingBranch(branchUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Training branch deleted successfully"));
    }

    @Operation(
            summary = "Get users assigned to training branch",
            description = "Retrieves all users that are assigned to a specific training branch within the organization. " +
                    "This includes users with any role/domain within the branch."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branch users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @GetMapping("/{uuid}/training-branches/{branchUuid}/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getBranchUsers(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to get users for. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid) {
        List<UserDTO> users = trainingBranchService.getBranchUsers(branchUuid);
        return ResponseEntity.ok(ApiResponse.success(users, "Branch users retrieved successfully"));
    }

    @Operation(
            summary = "Get users by training branch and domain",
            description = "Retrieves all users in the training branch filtered by their role/domain. " +
                    "This endpoint is useful for getting all instructors, students, or admins within a specific branch."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered branch users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid domain name")
    @GetMapping("/{uuid}/training-branches/{branchUuid}/users/domain/{domainName}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getBranchUsersByDomain(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to get users for. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Parameter(description = "Domain name to filter users by. Valid values: 'student', 'instructor', 'admin', 'organisation_user'",
                    example = "student", required = true)
            @PathVariable String domainName) {
        List<UserDTO> users = trainingBranchService.getBranchUsersByDomain(branchUuid, domainName);
        return ResponseEntity.ok(ApiResponse.success(users, "Branch users retrieved successfully"));
    }

    @Operation(
            summary = "Assign user to training branch",
            description = "Assigns a user to a specific training branch with a defined role. " +
                    "If the user is not already in the parent organization, creates organization membership first. " +
                    "If the user is already in the organization, updates their branch assignment."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User assigned to branch successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch or user not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid domain name")
    @PostMapping("/{uuid}/training-branches/{branchUuid}/users/{userUuid}")
    public ResponseEntity<ApiResponse<Void>> assignUserToBranch(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to assign the user to. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Parameter(description = "UUID of the user to assign to the training branch. Must be an existing user.",
                    example = "550e8400-e29b-41d4-a716-446655440003", required = true)
            @PathVariable UUID userUuid,
            @Parameter(description = "Role/domain name for the user in this branch. Valid values: 'student', 'instructor', 'admin', 'organisation_user'",
                    example = "student", required = true)
            @RequestParam("domain_name") String domainName) {
        trainingBranchService.assignUserToBranch(branchUuid, userUuid, domainName);
        return ResponseEntity.ok(ApiResponse.success(null, "User assigned to branch successfully"));
    }

    @Operation(
            summary = "Remove user from training branch",
            description = "Removes a user from a training branch. " +
                    "The user remains in the parent organization but loses branch-specific assignment."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User removed from branch successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch or user not found, or user not assigned to branch")
    @DeleteMapping("/{uuid}/training-branches/{branchUuid}/users/{userUuid}")
    public ResponseEntity<ApiResponse<Void>> removeUserFromBranch(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to remove the user from. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Parameter(description = "UUID of the user to remove from the training branch. Must be currently assigned to the branch.",
                    example = "550e8400-e29b-41d4-a716-446655440003", required = true)
            @PathVariable UUID userUuid) {
        trainingBranchService.removeUserFromBranch(branchUuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "User removed from branch successfully"));
    }

    @Operation(
            summary = "Create training branch invitation",
            description = "Creates and sends an email invitation for a user to join a specific training branch with a defined role. " +
                    "This is a specialized invitation that automatically determines the parent organization from the branch. " +
                    "The invitation email will include branch-specific information and location details."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Branch invitation created and email sent successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data: duplicate invitation, invalid domain, or invalid branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch, inviter user not found")
    @PostMapping("/{uuid}/training-branches/{branchUuid}/invitations")
    public ResponseEntity<ApiResponse<InvitationDTO>> createBranchInvitation(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch the user is being invited to join. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Parameter(description = "Email address of the person being invited to the training branch. Must be a valid email format.",
                    example = "jane.smith@example.com", required = true)
            @RequestParam("recipient_email") String recipientEmail,
            @Parameter(description = "Full name of the person being invited to the training branch. Used in email templates and records.",
                    example = "Jane Smith", required = true)
            @RequestParam("recipient_name") String recipientName,
            @Parameter(description = "Role/domain name being offered to the recipient within the training branch. Valid values: 'student', 'instructor', 'admin', 'organisation_user'",
                    example = "student", required = true)
            @RequestParam("domain_name") String domainName,
            @Parameter(description = "UUID of the user who is sending this branch invitation. Must be an existing user with appropriate permissions.",
                    example = "550e8400-e29b-41d4-a716-446655440004", required = true)
            @RequestParam("inviter_uuid") UUID inviterUuid,
            @Parameter(description = "Optional personal message or notes to include with the branch invitation email. Maximum 500 characters.",
                    example = "Join our downtown training center for hands-on learning!", required = false)
            @RequestParam(value = "notes", required = false) String notes) {
        InvitationDTO created = invitationService.createBranchInvitation(
                recipientEmail, recipientName, branchUuid, domainName, inviterUuid, notes);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Branch invitation created successfully"));
    }

    @Operation(
            summary = "Get all invitations for training branch",
            description = "Retrieves all invitations (regardless of status) that have been sent specifically for this training branch. " +
                    "This only includes branch-specific invitations, not general organization invitations. " +
                    "Results are ordered by creation date (most recent first) and include all invitation statuses."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Branch invitations retrieved successfully (may be empty list)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @GetMapping("/{uuid}/training-branches/{branchUuid}/invitations")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getBranchInvitations(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to retrieve invitations for. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid) {
        List<InvitationDTO> invitations = invitationService.getBranchInvitations(branchUuid);
        return ResponseEntity.ok(ApiResponse.success(invitations, "Branch invitations retrieved successfully"));
    }

    @Operation(
            summary = "Update point of contact for training branch",
            description = "Updates the point of contact user for a training branch. " +
                    "The POC must be either assigned to the branch or be a member of the parent organization."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Point of contact updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User is not eligible to be POC")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch or user not found")
    @PutMapping("/{uuid}/training-branches/{branchUuid}/poc/{pocUserUuid}")
    public ResponseEntity<ApiResponse<Void>> updatePointOfContact(
            @Parameter(description = "UUID of the organization that owns the training branch. Must be an existing organization.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "UUID of the training branch to update the POC for. Must be a branch within the specified organization.",
                    example = "550e8400-e29b-41d4-a716-446655440002", required = true)
            @PathVariable UUID branchUuid,
            @Parameter(description = "UUID of the user to set as point of contact. Must be assigned to the branch or be a member of the organization.",
                    example = "550e8400-e29b-41d4-a716-446655440003", required = true)
            @PathVariable UUID pocUserUuid) {
        trainingBranchService.updatePointOfContact(branchUuid, pocUserUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Point of contact updated successfully"));
    }
}