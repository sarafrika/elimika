package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationResultDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianDetailsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.MyInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicInvitationDTO;
import apps.sarafrika.elimika.tenancy.services.InvitationAcceptanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * The recipient's side of an invitation.
 * <p>
 * The token lookup is deliberately public: someone who has never had an Elimika account
 * must be able to see who is inviting them, and to what, before deciding whether to
 * register. Acting on the invitation always requires signing in first, and the signed-in
 * address must match the one the invitation was sent to.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@RestController
@RequestMapping(InvitationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invitations", description = "View and respond to an invitation you have received")
public class InvitationController {

    public static final String API_ROOT_PATH = "/api/v1/invitations";

    private final InvitationAcceptanceService acceptanceService;
    private final UserContextService userContextService;

    @Operation(
            summary = "Read an invitation from its link",
            description = "Public. Returns only what the recipient needs in order to decide: the " +
                    "organisation, the inviter, the role and the expiry. The recipient's address is " +
                    "masked and no other personal data is disclosed."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Link is not valid")
    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<PublicInvitationDTO>> getInvitationByToken(
            @Parameter(description = "Token from the emailed invitation link", required = true)
            @PathVariable String token) {

        PublicInvitationDTO invitation = acceptanceService.lookupByToken(token);
        return ResponseEntity.ok(ApiResponse.success(invitation, "Invitation retrieved successfully"));
    }

    @Operation(
            summary = "Accept an invitation from its link",
            description = "Creates the affiliation. If the accepting user turns out to be below the " +
                    "configured age gate, no affiliation is created - the invitation moves to " +
                    "awaiting guardian consent and guardian details must be supplied next."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation accepted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Date of birth required, or invitation no longer open")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Signed-in address does not match the invitation")
    @PostMapping("/token/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcceptInvitationResultDTO>> acceptInvitationByToken(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequestDTO request) {

        AcceptInvitationResultDTO result = acceptanceService.acceptByToken(
                token, request, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @Operation(summary = "Decline an invitation from its link")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation declined")
    @PostMapping("/token/{token}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> declineInvitationByToken(@PathVariable String token) {
        acceptanceService.declineByToken(token, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }

    @Operation(
            summary = "Nominate a guardian for a minor's invitation",
            description = "Supplies the guardian who will decide on the minor's behalf, and issues " +
                    "that guardian their own consent link. Captures only what is needed to reach " +
                    "them and to record the relationship."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Guardian recorded and consent requested")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invitation is not awaiting guardian details")
    @PostMapping("/token/{token}/guardian-details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PublicInvitationDTO>> submitGuardianDetails(
            @PathVariable String token,
            @Valid @RequestBody GuardianDetailsRequestDTO details) {

        PublicInvitationDTO invitation = acceptanceService.submitGuardianDetails(
                token, details, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(invitation,
                "We have asked your parent or guardian to approve this."));
    }

    @Operation(
            summary = "List invitations addressed to me",
            description = "Lets someone who never opened the email still find and act on an offer."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitations retrieved")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MyInvitationDTO>>> listMyInvitations() {
        List<MyInvitationDTO> invitations =
                acceptanceService.listForUser(userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(invitations, "Invitations retrieved successfully"));
    }

    @Operation(summary = "Accept an invitation from my inbox")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation accepted")
    @PostMapping("/{invitationUuid}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcceptInvitationResultDTO>> acceptInvitationFromInbox(
            @PathVariable UUID invitationUuid,
            @Valid @RequestBody AcceptInvitationRequestDTO request) {

        AcceptInvitationResultDTO result = acceptanceService.acceptByUuid(
                invitationUuid, request, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @Operation(summary = "Decline an invitation from my inbox")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation declined")
    @PostMapping("/{invitationUuid}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> declineInvitationFromInbox(@PathVariable UUID invitationUuid) {
        acceptanceService.declineByUuid(invitationUuid, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }
}
