package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationResultDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianConsentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicGuardianInvitationDTO;
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

/**
 * The guardian's side of a minor's invitation.
 * <p>
 * A minor cannot consent to an organisation affiliation for themselves, so their
 * acceptance only nominates a guardian. Nothing exists until the guardian acts here, on
 * their own link and under their own sign-in.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@RestController
@RequestMapping(GuardianInvitationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian Consent", description = "Approve or refuse a minor joining an organisation")
public class GuardianInvitationController {

    public static final String API_ROOT_PATH = "/api/v1/guardian-invitations";

    private final InvitationAcceptanceService acceptanceService;
    private final UserContextService userContextService;

    @Operation(
            summary = "Read a guardian consent request from its link",
            description = "Public, so a guardian with no Elimika account can see what they are being " +
                    "asked to approve before registering. Names the child and masks their address."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consent request retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Link is not valid")
    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<PublicGuardianInvitationDTO>> getGuardianInvitationByToken(
            @Parameter(description = "Token from the emailed consent link", required = true)
            @PathVariable String token) {

        PublicGuardianInvitationDTO request = acceptanceService.lookupGuardianRequestByToken(token);
        return ResponseEntity.ok(ApiResponse.success(request, "Consent request retrieved successfully"));
    }

    @Operation(
            summary = "Consent to the minor joining the organisation",
            description = "Creates the child's affiliation and establishes the guardian's own ongoing " +
                    "visibility of that child's learning at the chosen scope."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Consent recorded")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Signed-in address is not the nominated guardian")
    @PostMapping("/token/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcceptInvitationResultDTO>> grantGuardianConsent(
            @PathVariable String token,
            @Valid @RequestBody GuardianConsentRequestDTO request) {

        AcceptInvitationResultDTO result = acceptanceService.guardianConsent(
                token, request, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @Operation(summary = "Refuse the minor joining the organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refusal recorded")
    @PostMapping("/token/{token}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> declineGuardianConsent(@PathVariable String token) {
        acceptanceService.guardianDecline(token, userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(null, "Consent refused"));
    }
}
