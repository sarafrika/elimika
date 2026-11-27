package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.tenancy.dto.OrganisationInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsResultDTO;
import apps.sarafrika.elimika.tenancy.dto.SendParentInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.security.OrganisationSecurityService;
import apps.sarafrika.elimika.tenancy.services.OrganisationInvitationService;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Organisation-facing invitation management.
 * <p>
 * Every endpoint here is restricted to a manager of the organisation in question:
 * inviting sends email in the organisation's name, so it is not something any
 * authenticated user may do on its behalf.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@RestController
@RequestMapping(OrganisationInvitationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organisation Invitations", description = "Invite people to join an organisation")
public class OrganisationInvitationController {

    public static final String API_ROOT_PATH = "/api/v1/organisations/{organisationUuid}/invitations";

    private final OrganisationInvitationService invitationService;
    private final UserContextService userContextService;
    private final OrganisationSecurityService organisationSecurityService;

    @Operation(
            summary = "Invite people to join the organisation",
            description = "Creates a token-bearing offer for each recipient and emails it. " +
                    "No account is provisioned and no affiliation exists until the recipient - " +
                    "or, for a minor, their guardian - accepts. Recipients are processed " +
                    "independently, so the response reports each one."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitations processed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid domain, branch or class selection")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SendOrganisationInvitationsResultDTO>> sendOrganisationInvitations(
            @Parameter(description = "UUID of the inviting organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody SendOrganisationInvitationsRequestDTO request) {

        UUID callerUuid = requireOrganisationManager(organisationUuid);
        SendOrganisationInvitationsResultDTO result = invitationService.send(organisationUuid, request, callerUuid);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Invitations processed"));
    }

    @Operation(
            summary = "Invite parents to approve their children joining",
            description = "For students under 18 who have no email of their own. The organisation names each child "
                    + "and their parent; the parent is emailed a link to approve or decline. Nothing is created "
                    + "until the parent approves: the child then gets a learner profile with no login (or the "
                    + "parent's existing child with the same name and date of birth is used), joins as a student, "
                    + "and is linked to the parent, who gains the parent domain. One invitation per child, each "
                    + "reported individually; a child aged 18 or over, below the learner age gate, or already "
                    + "invited is reported as failed."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitations processed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid branch or class selection")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @PostMapping("/parents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SendOrganisationInvitationsResultDTO>> sendParentInvitations(
            @Parameter(description = "UUID of the inviting organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody SendParentInvitationsRequestDTO request) {

        UUID callerUuid = requireOrganisationManager(organisationUuid);
        SendOrganisationInvitationsResultDTO result = invitationService.sendToParents(organisationUuid, request, callerUuid);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Parent invitations processed"));
    }

    @Operation(
            summary = "List the organisation's invitations",
            description = "Newest first. Optionally filtered by status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitations retrieved")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrganisationInvitationDTO>>> listOrganisationInvitations(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Optional status filter, e.g. PENDING,ACCEPTED")
            @RequestParam(required = false) List<InvitationStatus> status) {

        requireOrganisationManager(organisationUuid);
        List<OrganisationInvitationDTO> invitations =
                invitationService.listForOrganisation(organisationUuid, status);

        return ResponseEntity.ok(ApiResponse.success(invitations, "Invitations retrieved successfully"));
    }

    @Operation(
            summary = "Withdraw a pending invitation",
            description = "Marks the invitation revoked and invalidates the emailed link immediately."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation revoked")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invitation is not pending")
    @PostMapping("/{invitationUuid}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganisationInvitationDTO>> revokeOrganisationInvitation(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID invitationUuid) {

        requireOrganisationManager(organisationUuid);
        OrganisationInvitationDTO revoked = invitationService.revoke(organisationUuid, invitationUuid);

        return ResponseEntity.ok(ApiResponse.success(revoked, "Invitation revoked successfully"));
    }

    @Operation(
            summary = "Resend a pending invitation",
            description = "Issues a fresh link and expiry. The previous link stops working."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation resent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invitation is not pending")
    @PostMapping("/{invitationUuid}/resend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganisationInvitationDTO>> resendOrganisationInvitation(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID invitationUuid) {

        requireOrganisationManager(organisationUuid);
        OrganisationInvitationDTO resent = invitationService.resend(organisationUuid, invitationUuid);

        return ResponseEntity.ok(ApiResponse.success(resent, "Invitation resent successfully"));
    }

    /**
     * Confirms the caller manages this organisation, and returns their user UUID.
     * <p>
     * Delegates to the tenant-scoped predicate rather than repeating it: platform admins are
     * allowed through for support purposes, everyone else must hold an org-scoped
     * {@code admin} mapping for this organisation specifically.
     * Answering it there also means the three membership queries this used to issue per call are
     * resolved once for the whole request.
     */
    private UUID requireOrganisationManager(UUID organisationUuid) {
        UUID callerUuid = userContextService.getCurrentUserUuid();

        if (!organisationSecurityService.canManageOrganisation(organisationUuid)) {
            log.warn("User {} attempted to manage invitations for organisation {} without a manager role",
                    callerUuid, organisationUuid);
            throw new AccessDeniedException("You do not manage this organisation.");
        }
        return callerUuid;
    }
}
