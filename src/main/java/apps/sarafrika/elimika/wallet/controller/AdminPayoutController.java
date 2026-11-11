package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.wallet.dto.PayoutSettingsDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutSummaryDTO;
import apps.sarafrika.elimika.wallet.dto.RejectWithdrawalRequestDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalDTO;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WalletCheck;
import apps.sarafrika.elimika.wallet.service.WithdrawalWalletChecks;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The admin Payouts page: the withdrawal queue, decisions on flagged ones, and the payout rules. */
@RestController
@RequestMapping("/api/v1/admin/payouts")
@Tag(name = "Admin payouts", description = "Review withdrawals and set payout rules")
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class AdminPayoutController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalRepository withdrawalRepository;
    private final PayoutSettingsService settingsService;
    private final PayoutBatchService batchService;
    private final MobileMoneyPayoutGateway gateway;
    private final WithdrawalWalletChecks walletChecks;

    @GetMapping("/summary")
    @Operation(operationId = "getPayoutSummary", summary = "Headline payout figures")
    public ResponseEntity<ApiResponse<PayoutSummaryDTO>> summary() {
        Optional<MobileMoneyPayoutGateway.PayoutFloat> payoutFloat = gateway.currentFloat();
        PayoutSummaryDTO summary = new PayoutSummaryDTO(
                withdrawalRepository.countByStatus(WithdrawalStatus.NEEDS_REVIEW),
                withdrawalService.queue(List.of(WithdrawalStatus.NEEDS_REVIEW), Pageable.unpaged()).stream()
                        .map(w -> w.getAmount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                withdrawalRepository.countByStatus(WithdrawalStatus.SENDING)
                        + withdrawalRepository.countByStatus(WithdrawalStatus.PROCESSING),
                withdrawalRepository.sumNetCompletedSince(WithdrawalStatus.PAID,
                        LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC)),
                withdrawalRepository.countByStatus(WithdrawalStatus.FAILED),
                payoutFloat.map(MobileMoneyPayoutGateway.PayoutFloat::utilityAccountFunds).orElse(null),
                payoutFloat.map(MobileMoneyPayoutGateway.PayoutFloat::reportedAt).orElse(null),
                gateway.isConfigured());
        return ResponseEntity.ok(ApiResponse.success(summary, "Payout summary retrieved successfully"));
    }

    @GetMapping("/withdrawals")
    @Operation(operationId = "listAdminWithdrawals", summary = "List withdrawals across all wallets",
            description = "Newest first; filter by status, e.g. NEEDS_REVIEW. With unbatched=true, withdrawals "
                    + "already in a payout batch are left out, which gives the queue a new batch is built from. "
                    + "Each row carries wallet_check, its earner's wallet read now, and wallet_flags derived from it.")
    public ResponseEntity<ApiResponse<PagedDTO<WithdrawalDTO>>> list(
            @RequestParam(value = "status", required = false) List<WithdrawalStatus> statuses,
            @RequestParam(value = "unbatched", required = false, defaultValue = "false") boolean unbatched,
            Pageable pageable) {
        Page<Withdrawal> withdrawals = withdrawalService.queue(statuses, unbatched, pageable);
        Map<UUID, WalletCheck> checks = walletChecks.check(withdrawals.getContent());
        Page<WithdrawalDTO> page = withdrawals.map(w -> WithdrawalDTO.from(w, checks.get(w.getUuid())));
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Withdrawals retrieved successfully"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/approve")
    @Operation(operationId = "approveAdminWithdrawal", summary = "Put one withdrawal up for approval on its own",
            description = "Does not send money. Creates a one-row payout batch holding this withdrawal, submitted by "
                    + "the caller and waiting for approval (AWAITING_APPROVAL), so a different admin must still approve "
                    + "that batch before M-Pesa pays it. The withdrawal stays NEEDS_REVIEW and comes back with its "
                    + "payout_batch_uuid set. Refused when the withdrawal is already in a batch.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> approve(@PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(checked(batchService.submitSingle(withdrawalUuid)),
                "Withdrawal submitted for approval in a payout batch"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/reject")
    @Operation(operationId = "rejectAdminWithdrawal", summary = "Reject a withdrawal and return its money",
            description = "The reason is shown to the earner. A withdrawal in a draft payout batch leaves the batch; "
                    + "one in a batch waiting for approval or released cannot be rejected on its own (409).")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> reject(@PathVariable UUID withdrawalUuid,
                                                             @Valid @RequestBody RejectWithdrawalRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(checked(
                withdrawalService.adminReject(withdrawalUuid, request.reason())), "Withdrawal rejected"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/check")
    @Operation(operationId = "checkAdminWithdrawal", summary = "Ask M-Pesa about a withdrawal now",
            description = "Sends a cleared withdrawal outside a payout batch, or reads the result of one in flight. "
                    + "Batch rows are sent only by the batch sender, which paces them.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> check(@PathVariable UUID withdrawalUuid) {
        boolean batched = withdrawalRepository.findByUuid(withdrawalUuid)
                .map(w -> w.getPayoutBatchUuid() != null).orElse(false);
        if (!batched) {
            withdrawalService.send(withdrawalUuid);
        }
        withdrawalService.refresh(withdrawalUuid);
        return ResponseEntity.ok(ApiResponse.success(checked(withdrawalRepository.findByUuid(withdrawalUuid)
                .orElseThrow(() -> new apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException(
                        "Withdrawal not found."))), "Withdrawal checked"));
    }

    @GetMapping("/settings")
    @Operation(operationId = "getPayoutSettings", summary = "Get the payout rules")
    public ResponseEntity<ApiResponse<PayoutSettingsDTO>> settings() {
        return ResponseEntity.ok(ApiResponse.success(PayoutSettingsDTO.from(settingsService.current()),
                "Payout settings retrieved successfully"));
    }

    @PutMapping("/settings")
    @Operation(operationId = "updatePayoutSettings", summary = "Change the payout rules",
            description = "Apply to withdrawals requested from now on.")
    public ResponseEntity<ApiResponse<PayoutSettingsDTO>> updateSettings(@Valid @RequestBody PayoutSettingsDTO request,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(PayoutSettingsDTO.from(settingsService.update(request.minAmount(),
                request.maxAmount(), request.dailyLimit(), request.reviewLimit(), request.secondApprovalLimit(),
                request.feePayer(), request.earnerFee(), request.requireAdminApproval(),
                authentication == null ? "SYSTEM" : authentication.getName())),
                "Payout settings saved"));
    }

    private WithdrawalDTO checked(Withdrawal withdrawal) {
        return WithdrawalDTO.from(withdrawal, walletChecks.check(List.of(withdrawal)).get(withdrawal.getUuid()));
    }
}
