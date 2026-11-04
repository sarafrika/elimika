package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.wallet.dto.ApprovePayoutBatchRequestDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutBatchDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutBatchDetailDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutBatchWithdrawalsRequestDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutFloatDTO;
import apps.sarafrika.elimika.wallet.dto.RejectPayoutBatchRequestDTO;
import apps.sarafrika.elimika.wallet.dto.WithdrawalDTO;
import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import apps.sarafrika.elimika.wallet.service.WalletCheck;
import apps.sarafrika.elimika.wallet.service.WithdrawalWalletChecks;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The admin Payouts page's batches: build, submit, approve against the float, and follow them to M-Pesa. */
@RestController
@RequestMapping("/api/v1/admin/payouts/batches")
@Tag(name = "Admin payouts", description = "Review withdrawals and set payout rules")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class AdminPayoutBatchController {

    private final PayoutBatchService batchService;
    private final DomainSecurityService domainSecurityService;
    private final MobileMoneyPayoutGateway gateway;
    private final WithdrawalWalletChecks walletChecks;

    @GetMapping
    @Operation(operationId = "listPayoutBatches", summary = "List payout batches",
            description = "Newest first; filter by status, e.g. AWAITING_APPROVAL.")
    public ResponseEntity<ApiResponse<PagedDTO<PayoutBatchDTO>>> list(
            @RequestParam(value = "status", required = false) List<PayoutBatchStatus> statuses, Pageable pageable) {
        Page<PayoutBatch> batches = batchService.list(statuses, pageable);
        List<UUID> uuids = batches.getContent().stream().map(PayoutBatch::getUuid).toList();
        Map<UUID, PayoutBatchService.RowCounts> counts = batchService.counts(uuids);
        Map<UUID, PayoutBatchService.WalletIssues> walletIssues = batchService.walletIssues(uuids);
        UUID me = domainSecurityService.getCurrentUserUuid();
        Page<PayoutBatchDTO> page = batches.map(b -> PayoutBatchDTO.from(b, counts.get(b.getUuid()),
                walletIssues.get(b.getUuid()), me));
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Payout batches retrieved successfully"));
    }

    @PostMapping
    @Operation(operationId = "createPayoutBatch", summary = "Start a draft payout batch",
            description = "Optionally with withdrawals waiting for review (NEEDS_REVIEW) that are in no other batch.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> create(
            @Valid @RequestBody(required = false) PayoutBatchWithdrawalsRequestDTO request) {
        PayoutBatch batch = batchService.create(request == null || request.withdrawalUuids() == null
                ? List.of() : request.withdrawalUuids());
        return ResponseEntity.ok(ApiResponse.success(dto(batch), "Payout batch created"));
    }

    @GetMapping("/{batchUuid}")
    @Operation(operationId = "getPayoutBatch", summary = "Get a payout batch with its withdrawals and the payout float",
            description = "payout_float is read from M-Pesa on each call and is null when unknown. Each row carries "
                    + "wallet_check, its earner's wallet read now, and wallet_flags derived from it; the batch's "
                    + "wallet_issue_count counts the rows with a wallet flag.")
    public ResponseEntity<ApiResponse<PayoutBatchDetailDTO>> get(@PathVariable UUID batchUuid) {
        PayoutBatch batch = batchService.get(batchUuid);
        List<Withdrawal> withdrawals = batchService.rows(batchUuid);
        Map<UUID, WalletCheck> checks = walletChecks.check(withdrawals);
        List<WithdrawalDTO> rows = withdrawals.stream().map(w -> WithdrawalDTO.from(w, checks.get(w.getUuid()))).toList();
        PayoutFloatDTO payoutFloat = null;
        try {
            payoutFloat = gateway.currentFloat().map(PayoutFloatDTO::from).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Could not read the payout float: {}", ex.getMessage());
        }
        PayoutBatchDTO summary = PayoutBatchDTO.from(batch, batchService.counts(List.of(batchUuid)).get(batchUuid),
                PayoutBatchService.WalletIssues.of(checks.values()), domainSecurityService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(new PayoutBatchDetailDTO(summary, rows, payoutFloat),
                "Payout batch retrieved successfully"));
    }

    @PostMapping("/{batchUuid}/withdrawals")
    @Operation(operationId = "addPayoutBatchWithdrawals", summary = "Add withdrawals to a draft batch",
            description = "Each must be waiting for review (NEEDS_REVIEW) and in no other batch.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> add(@PathVariable UUID batchUuid,
                                                           @Valid @RequestBody PayoutBatchWithdrawalsRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.addWithdrawals(batchUuid,
                request.withdrawalUuids() == null ? List.of() : request.withdrawalUuids())), "Withdrawals added"));
    }

    @DeleteMapping("/{batchUuid}/withdrawals/{withdrawalUuid}")
    @Operation(operationId = "removePayoutBatchWithdrawal", summary = "Take a withdrawal out of a draft batch",
            description = "It goes back to the review queue, still held.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> remove(@PathVariable UUID batchUuid,
                                                              @PathVariable UUID withdrawalUuid) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.removeWithdrawal(batchUuid, withdrawalUuid)),
                "Withdrawal removed"));
    }

    @PostMapping("/{batchUuid}/submit")
    @Operation(operationId = "submitPayoutBatch", summary = "Submit a draft batch for approval",
            description = "A different admin from whoever built or submitted it must then approve it.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> submit(@PathVariable UUID batchUuid) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.submit(batchUuid)), "Payout batch submitted"));
    }

    @PostMapping("/{batchUuid}/approve")
    @Operation(operationId = "approvePayoutBatch", summary = "Approve a batch and release it to M-Pesa",
            description = "Refused (409) for whoever built or submitted the batch, when the batch total exceeds the "
                    + "payout float's utility funds, when the float is unknown unless override_unknown_float is "
                    + "true, and when any row carries a wallet flag (wallet_issue_count above zero) unless "
                    + "acknowledge_wallet_issues is true, which the approval audit records. On approval every row "
                    + "moves to SENDING and is paid a few at a time.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> approve(@PathVariable UUID batchUuid,
                                                               @RequestBody(required = false) ApprovePayoutBatchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.approve(batchUuid,
                request != null && request.overrideUnknownFloat(), request != null && request.acknowledgeWalletIssues())),
                "Payout batch approved"));
    }

    @PostMapping("/{batchUuid}/reject")
    @Operation(operationId = "rejectPayoutBatch", summary = "Reject a batch waiting for approval",
            description = "Its withdrawals go back to the review queue, still held; reject them one by one to "
                    + "return the money.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> reject(@PathVariable UUID batchUuid,
                                                              @Valid @RequestBody RejectPayoutBatchRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.reject(batchUuid, request.reason())),
                "Payout batch rejected"));
    }

    @PostMapping("/{batchUuid}/return-to-draft")
    @Operation(operationId = "returnPayoutBatchToDraft", summary = "Send a batch waiting for approval back to draft",
            description = "Any admin may; it must be submitted again before it can be approved.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> returnToDraft(@PathVariable UUID batchUuid) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.returnToDraft(batchUuid)),
                "Payout batch returned to draft"));
    }

    @PostMapping("/{batchUuid}/cancel")
    @Operation(operationId = "cancelPayoutBatch", summary = "Cancel a draft batch",
            description = "Its withdrawals go back to the review queue, still held.")
    public ResponseEntity<ApiResponse<PayoutBatchDTO>> cancel(@PathVariable UUID batchUuid) {
        return ResponseEntity.ok(ApiResponse.success(dto(batchService.cancel(batchUuid)), "Payout batch cancelled"));
    }

    private PayoutBatchDTO dto(PayoutBatch batch) {
        return PayoutBatchDTO.from(batch, batchService.counts(List.of(batch.getUuid())).get(batch.getUuid()),
                batchService.walletIssues(List.of(batch.getUuid())).get(batch.getUuid()),
                domainSecurityService.getCurrentUserUuid());
    }
}
