package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.wallet.dto.PayoutApproverDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutApproverRequestDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodCreatedDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodRequestDTO;
import apps.sarafrika.elimika.wallet.dto.VerificationCodeDTO;
import apps.sarafrika.elimika.wallet.dto.VerifyCodeRequestDTO;
import apps.sarafrika.elimika.wallet.service.PayoutMethodService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** An organisation's payout numbers (its admins see them, its payout approvers change them) and approvers. */
@RestController
@RequestMapping("/api/v1/organisations/{organisationUuid}/wallet")
@Tag(name = "Organisation payouts", description = "Where an organisation's withdrawals go, and who may handle them")
@RequiredArgsConstructor
public class OrganisationPayoutController {

    private static final String READ = "@organisationSecurityService.canManageOrganisation(#organisationUuid)";
    private static final String CHANGE = "@walletSecurityService.canManageOrganisationPayouts(#organisationUuid)";
    private static final String APPROVERS = "@walletSecurityService.canManagePayoutApprovers(#organisationUuid)";

    private final PayoutMethodService payoutMethodService;

    @GetMapping("/payout-methods")
    @PreAuthorize(READ)
    @Operation(operationId = "listOrganisationPayoutMethods", summary = "List an organisation's payout numbers")
    public ResponseEntity<ApiResponse<List<PayoutMethodDTO>>> list(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(payoutMethodService.list(WalletOwner.organisation(organisationUuid))
                .stream().map(PayoutMethodDTO::from).toList(), "Payout methods retrieved successfully"));
    }

    @PostMapping("/payout-methods")
    @PreAuthorize(CHANGE)
    @Operation(operationId = "addOrganisationPayoutMethod", summary = "Add an M-Pesa number for the organisation",
            description = "Payout approvers only. A six-digit code is sent to the number.")
    public ResponseEntity<ApiResponse<PayoutMethodCreatedDTO>> add(@PathVariable UUID organisationUuid,
                                                                   @Valid @RequestBody PayoutMethodRequestDTO request) {
        PayoutMethodService.AddedPayoutMethod added = payoutMethodService.add(WalletOwner.organisation(organisationUuid),
                request.msisdn(), request.label(), request.confirmsRegisteredName());
        return ResponseEntity.ok(ApiResponse.success(new PayoutMethodCreatedDTO(PayoutMethodDTO.from(added.method()),
                VerificationCodeDTO.from(added.verification())), "Verification code sent"));
    }

    @PostMapping("/payout-methods/{methodUuid}/verification-code")
    @PreAuthorize(CHANGE)
    @Operation(operationId = "resendOrganisationPayoutMethodCode", summary = "Send a new verification code")
    public ResponseEntity<ApiResponse<VerificationCodeDTO>> resend(@PathVariable UUID organisationUuid,
                                                                   @PathVariable UUID methodUuid) {
        return ResponseEntity.ok(ApiResponse.success(VerificationCodeDTO.from(payoutMethodService.resendCode(
                WalletOwner.organisation(organisationUuid), methodUuid)), "Verification code sent"));
    }

    @PostMapping("/payout-methods/{methodUuid}/verify")
    @PreAuthorize(CHANGE)
    @Operation(operationId = "verifyOrganisationPayoutMethod", summary = "Confirm an organisation number with its code")
    public ResponseEntity<ApiResponse<PayoutMethodDTO>> verify(@PathVariable UUID organisationUuid,
                                                               @PathVariable UUID methodUuid,
                                                               @Valid @RequestBody VerifyCodeRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(PayoutMethodDTO.from(payoutMethodService.verify(
                WalletOwner.organisation(organisationUuid), methodUuid, request.code())), "Number verified"));
    }

    @PutMapping("/payout-methods/{methodUuid}/default")
    @PreAuthorize(CHANGE)
    @Operation(operationId = "makeOrganisationPayoutMethodDefault", summary = "Make a verified number the default")
    public ResponseEntity<ApiResponse<PayoutMethodDTO>> makeDefault(@PathVariable UUID organisationUuid,
                                                                    @PathVariable UUID methodUuid) {
        return ResponseEntity.ok(ApiResponse.success(PayoutMethodDTO.from(payoutMethodService.makeDefault(
                WalletOwner.organisation(organisationUuid), methodUuid)), "Default payout number updated"));
    }

    @DeleteMapping("/payout-methods/{methodUuid}")
    @PreAuthorize(CHANGE)
    @Operation(operationId = "removeOrganisationPayoutMethod", summary = "Remove an organisation payout number")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID organisationUuid, @PathVariable UUID methodUuid) {
        payoutMethodService.remove(WalletOwner.organisation(organisationUuid), methodUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Payout number removed"));
    }

    @GetMapping("/payout-approvers")
    @PreAuthorize(READ)
    @Operation(operationId = "listPayoutApprovers", summary = "List the organisation's payout approvers")
    public ResponseEntity<ApiResponse<List<PayoutApproverDTO>>> approvers(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(payoutMethodService.approvers(organisationUuid).stream()
                .map(PayoutApproverDTO::from).toList(), "Payout approvers retrieved successfully"));
    }

    @PostMapping("/payout-approvers")
    @PreAuthorize(APPROVERS)
    @Operation(operationId = "addPayoutApprover", summary = "Name an admin as payout approver",
            description = "Any admin of the organisation may do this; the person named must be an admin too.")
    public ResponseEntity<ApiResponse<PayoutApproverDTO>> addApprover(@PathVariable UUID organisationUuid,
                                                                      @Valid @RequestBody PayoutApproverRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(PayoutApproverDTO.from(
                payoutMethodService.addApprover(organisationUuid, request.userUuid())), "Payout approver added"));
    }

    @DeleteMapping("/payout-approvers/{userUuid}")
    @PreAuthorize(APPROVERS)
    @Operation(operationId = "removePayoutApprover", summary = "Remove a payout approver")
    public ResponseEntity<ApiResponse<Void>> removeApprover(@PathVariable UUID organisationUuid, @PathVariable UUID userUuid) {
        payoutMethodService.removeApprover(organisationUuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Payout approver removed"));
    }
}
