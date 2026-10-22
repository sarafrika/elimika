package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.wallet.dto.PayoutSettingsDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutSummaryDTO;
import apps.sarafrika.elimika.wallet.dto.RejectWithdrawalRequestDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalDTO;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
    private final MobileMoneyPayoutGateway gateway;

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
            description = "Newest first; filter by status, e.g. NEEDS_REVIEW.")
    public ResponseEntity<ApiResponse<PagedDTO<WithdrawalDTO>>> list(
            @RequestParam(value = "status", required = false) List<WithdrawalStatus> statuses, Pageable pageable) {
        Page<WithdrawalDTO> page = withdrawalService.queue(statuses, pageable).map(WithdrawalDTO::from);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Withdrawals retrieved successfully"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/approve")
    @Operation(operationId = "approveAdminWithdrawal", summary = "Approve a flagged withdrawal and send it")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> approve(@PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(withdrawalService.adminApprove(withdrawalUuid)),
                "Withdrawal approved"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/reject")
    @Operation(operationId = "rejectAdminWithdrawal", summary = "Reject a withdrawal and return its money",
            description = "The reason is shown to the earner.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> reject(@PathVariable UUID withdrawalUuid,
                                                             @Valid @RequestBody RejectWithdrawalRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(
                withdrawalService.adminReject(withdrawalUuid, request.reason())), "Withdrawal rejected"));
    }

    @PostMapping("/withdrawals/{withdrawalUuid}/check")
    @Operation(operationId = "checkAdminWithdrawal", summary = "Ask M-Pesa about a withdrawal now",
            description = "Sends a cleared withdrawal, or reads the result of one in flight.")
    public ResponseEntity<ApiResponse<WithdrawalDTO>> check(@PathVariable UUID withdrawalUuid) {
        withdrawalService.send(withdrawalUuid);
        withdrawalService.refresh(withdrawalUuid);
        return ResponseEntity.ok(ApiResponse.success(WithdrawalDTO.from(withdrawalRepository.findByUuid(withdrawalUuid)
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
                request.feePayer(), request.earnerFee(), authentication == null ? "SYSTEM" : authentication.getName())),
                "Payout settings saved"));
    }
}
