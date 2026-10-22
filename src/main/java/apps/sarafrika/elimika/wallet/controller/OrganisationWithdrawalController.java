package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.wallet.dto.VerificationCodeDTO;
import apps.sarafrika.elimika.wallet.dto.VerifyCodeRequestDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalCreatedDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalRequestDTO;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** An organisation's withdrawals to M-Pesa, by its payout approvers. */
@RestController
@RequestMapping("/api/v1/organisations/{organisationUuid}/wallet/withdrawals")
@Tag(name = "Organisation withdrawals", description = "An organisation's withdrawals to M-Pesa, by its payout approvers")
@RequiredArgsConstructor
public class OrganisationWithdrawalController {

    private static final String READ = "@organisationSecurityService.canManageOrganisation(#organisationUuid)";
    private static final String WRITE = "@walletSecurityService.canWithdrawFromOrganisation(#organisationUuid)";

    private final WithdrawalService withdrawalService;

    @GetMapping
    @PreAuthorize(READ)
    @Operation(operationId = "listOrganisationWithdrawals", summary = "List withdrawals", description = "Newest first.")
    public ResponseEntity<ApiResponse<PagedDTO<WithdrawalDTO>>> list(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "status", required = false) List<WithdrawalStatus> statuses,
            Pageable pageable) {
        Page<WithdrawalDTO> page = withdrawalService.list(WalletOwner.organisation(organisationUuid), statuses, pageable).map(WithdrawalDTO::from);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Withdrawals retrieved successfully"));
    }

    @GetMapping("/{withdrawalUuid}")
    @PreAuthorize(READ)
    @Operation(operationId = "getOrganisationWithdrawal", summary = "Get one withdrawal")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> get(@PathVariable UUID organisationUuid, @PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(
                withdrawalService.get(WalletOwner.organisation(organisationUuid), withdrawalUuid)), "Withdrawal retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize(WRITE)
    @Operation(operationId = "requestOrganisationWithdrawal", summary = "Start a withdrawal to a verified M-Pesa number",
            description = "Nothing moves until the code sent by SMS to that number is confirmed.")
    public ResponseEntity<ApiResponse<WithdrawalCreatedDTO>> request(@PathVariable UUID organisationUuid,
                                                                     @Valid @RequestBody WithdrawalRequestDTO request) {
        WithdrawalService.RequestedWithdrawal requested = withdrawalService.request(WalletOwner.organisation(organisationUuid), request.amount(),
                request.payoutMethodUuid(), request.idempotencyKey());
        return ResponseEntity.ok(ApiResponse.success(new WithdrawalCreatedDTO(WithdrawalDTO.from(requested.withdrawal()),
                requested.verification() == null ? null : VerificationCodeDTO.from(requested.verification())),
                "Confirmation code sent"));
    }

    @PostMapping("/{withdrawalUuid}/verification-code")
    @PreAuthorize(WRITE)
    @Operation(operationId = "resendOrganisationWithdrawalCode", summary = "Send a new confirmation code")
    public ResponseEntity<ApiResponse<VerificationCodeDTO>> resend(@PathVariable UUID organisationUuid, @PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(VerificationCodeDTO.from(
                withdrawalService.resendCode(WalletOwner.organisation(organisationUuid), withdrawalUuid)), "Confirmation code sent"));
    }

    @PostMapping("/{withdrawalUuid}/confirm")
    @PreAuthorize(WRITE)
    @Operation(operationId = "confirmOrganisationWithdrawal", summary = "Confirm a withdrawal with its SMS code",
            description = "The amount leaves the balance now; it comes back if M-Pesa fails or an admin rejects it.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> confirm(@PathVariable UUID organisationUuid, @PathVariable UUID withdrawalUuid,
                                                              @Valid @RequestBody VerifyCodeRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(
                withdrawalService.confirm(WalletOwner.organisation(organisationUuid), withdrawalUuid, request.code())), "Withdrawal confirmed"));
    }

    @PostMapping("/{withdrawalUuid}/cancel")
    @PreAuthorize(WRITE)
    @Operation(operationId = "cancelOrganisationWithdrawal", summary = "Cancel a withdrawal that has not been sent")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> cancel(@PathVariable UUID organisationUuid, @PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(
                withdrawalService.cancel(WalletOwner.organisation(organisationUuid), withdrawalUuid)), "Withdrawal cancelled"));
    }

    @PostMapping("/{withdrawalUuid}/approve")
    @PreAuthorize(WRITE)
    @Operation(operationId = "approveOrganisationWithdrawal", summary = "Agree as the second payout approver",
            description = "Must be a different approver from the one who asked.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> approve(@PathVariable UUID organisationUuid,
                                                              @PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(
                withdrawalService.approveAsSecond(WalletOwner.organisation(organisationUuid), withdrawalUuid)),
                "Withdrawal approved"));
    }
}
