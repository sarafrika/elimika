package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutFloat;
import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.PayoutBatchRepository;
import apps.sarafrika.elimika.wallet.repository.WalletAuditEventRepository;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PayoutBatchServiceImpl implements PayoutBatchService {

    private static final Set<WithdrawalStatus> IN_FLIGHT = Set.of(WithdrawalStatus.SENDING, WithdrawalStatus.PROCESSING);

    private final PayoutBatchRepository batchRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WalletAuditEventRepository auditRepository;
    private final DomainSecurityService domainSecurityService;
    private final MobileMoneyPayoutGateway gateway;
    private final TransactionTemplate transactions;

    public PayoutBatchServiceImpl(PayoutBatchRepository batchRepository, WithdrawalRepository withdrawalRepository,
                                  WalletAuditEventRepository auditRepository, DomainSecurityService domainSecurityService,
                                  MobileMoneyPayoutGateway gateway, PlatformTransactionManager transactionManager) {
        this.batchRepository = batchRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.auditRepository = auditRepository;
        this.domainSecurityService = domainSecurityService;
        this.gateway = gateway;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutBatch> list(Collection<PayoutBatchStatus> statuses, Pageable pageable) {
        Collection<PayoutBatchStatus> wanted = statuses == null || statuses.isEmpty()
                ? Arrays.asList(PayoutBatchStatus.values()) : statuses;
        return batchRepository.findByStatusIn(wanted, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PayoutBatch get(UUID batchUuid) {
        return batchRepository.findByUuid(batchUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payout batch not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> rows(UUID batchUuid) {
        return withdrawalRepository.findByPayoutBatchUuid(batchUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, RowCounts> counts(Collection<UUID> batchUuids) {
        Map<UUID, RowCounts> counts = new HashMap<>();
        if (batchUuids == null || batchUuids.isEmpty()) {
            return counts;
        }
        for (Object[] row : withdrawalRepository.countByBatchAndStatus(batchUuids)) {
            UUID batch = (UUID) row[0];
            WithdrawalStatus status = (WithdrawalStatus) row[1];
            long n = ((Number) row[2]).longValue();
            RowCounts c = counts.getOrDefault(batch, RowCounts.NONE);
            counts.put(batch, new RowCounts(
                    c.paid() + (status == WithdrawalStatus.PAID ? n : 0),
                    c.failed() + (status == WithdrawalStatus.FAILED ? n : 0),
                    c.inFlight() + (IN_FLIGHT.contains(status) ? n : 0)));
        }
        return counts;
    }

    @Override
    @Transactional
    public PayoutBatch create(Collection<UUID> withdrawalUuids) {
        UUID actor = domainSecurityService.getCurrentUserUuid();
        PayoutBatch batch = new PayoutBatch();
        batch.setStatus(PayoutBatchStatus.DRAFT);
        batch.setCreatedByUserUuid(actor);
        batch.setCreatedByName(actorName());
        batch.setTotalAmount(BigDecimal.ZERO);
        batchRepository.saveAndFlush(batch);
        log.info("Payout batch {} created by {}", batch.getUuid(), actor);
        return add(batch, withdrawalUuids, actor);
    }

    @Override
    @Transactional
    public PayoutBatch addWithdrawals(UUID batchUuid, Collection<UUID> withdrawalUuids) {
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.DRAFT, "Rows can only be added to a draft batch.");
        return add(batch, withdrawalUuids, domainSecurityService.getCurrentUserUuid());
    }

    @Override
    @Transactional
    public PayoutBatch removeWithdrawal(UUID batchUuid, UUID withdrawalUuid) {
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.DRAFT, "Rows can only be removed from a draft batch.");
        Withdrawal withdrawal = withdrawalRepository.lockByUuid(withdrawalUuid)
                .filter(w -> batch.getUuid().equals(w.getPayoutBatchUuid()))
                .orElseThrow(() -> new ResourceNotFoundException("That withdrawal is not in this batch."));
        withdrawal.setPayoutBatchUuid(null);
        withdrawalRepository.save(withdrawal);
        recount(batch);
        audit(batch, withdrawal, WalletAuditEventType.PAYOUT_BATCH_ROW_REMOVED, domainSecurityService.getCurrentUserUuid(),
                "Removed from a payout batch", Map.of());
        return batchRepository.save(batch);
    }

    @Override
    @Transactional
    public PayoutBatch submit(UUID batchUuid) {
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.DRAFT, "Only a draft batch can be submitted.");
        List<Withdrawal> rows = withdrawalRepository.lockByPayoutBatchUuid(batch.getUuid());
        if (rows.isEmpty()) {
            throw new IllegalStateException("Add at least one withdrawal before submitting the batch.");
        }
        UUID actor = domainSecurityService.getCurrentUserUuid();
        recount(batch);
        batch.setStatus(PayoutBatchStatus.AWAITING_APPROVAL);
        batch.setSubmittedByUserUuid(actor);
        batch.setSubmittedByName(actorName());
        batch.setSubmittedAt(now());
        batchRepository.save(batch);
        rows.forEach(w -> audit(batch, w, WalletAuditEventType.PAYOUT_BATCH_SUBMITTED, actor,
                "Payout batch submitted for approval", Map.of()));
        return batch;
    }

    @Override
    public PayoutBatch approve(UUID batchUuid, boolean overrideUnknownFloat) {
        UUID actor = domainSecurityService.getCurrentUserUuid();
        String actorName = actorName();
        // Fail fast before asking M-Pesa; everything is checked again under the lock.
        requireApprovable(get(batchUuid), actor);
        Optional<PayoutFloat> payoutFloat = readFloat();
        return transactions.execute(status -> {
            PayoutBatch batch = locked(batchUuid);
            requireApprovable(batch, actor);
            List<Withdrawal> rows = withdrawalRepository.lockByPayoutBatchUuid(batch.getUuid());
            if (rows.isEmpty()) {
                throw new IllegalStateException("This batch has no withdrawals to approve.");
            }
            rows.stream().filter(w -> w.getStatus() != WithdrawalStatus.NEEDS_REVIEW).findFirst().ifPresent(w -> {
                throw new IllegalStateException("Withdrawal " + w.getReference() + " is " + label(w.getStatus())
                        + "; remove it from the batch first.");
            });
            recount(batch);
            BigDecimal utility = payoutFloat.map(PayoutFloat::utilityAccountFunds).orElse(null);
            if (utility == null && !overrideUnknownFloat) {
                throw new IllegalStateException("The payout float is unknown right now, so this batch of "
                        + WithdrawalServiceImpl.kes(batch.getTotalAmount()) + " cannot be checked against it. "
                        + "Try again shortly, or approve with override_unknown_float to release it anyway.");
            }
            if (utility != null && batch.getTotalAmount().compareTo(utility) > 0) {
                throw new IllegalStateException("The payout float is " + WithdrawalServiceImpl.kes(utility)
                        + " but this batch needs " + WithdrawalServiceImpl.kes(batch.getTotalAmount())
                        + ". Top up the float or remove rows before approving.");
            }
            OffsetDateTime now = now();
            batch.setApprovedByUserUuid(actor);
            batch.setApprovedByName(actorName);
            batch.setApprovedAt(now);
            batch.setFloatAtApproval(utility);
            batch.setFloatOverride(utility == null);
            batch.setStatus(PayoutBatchStatus.RELEASED);
            batch.setReleasedAt(now);
            batchRepository.save(batch);
            Map<String, Object> extra = new HashMap<>();
            extra.put("override_unknown_float", utility == null);
            payoutFloat.map(PayoutFloat::reportedAt).ifPresent(at -> extra.put("float_reported_at", at.toString()));
            for (Withdrawal w : rows) {
                w.setReviewedByUserUuid(actor);
                w.setReviewedAt(now);
                w.setStatus(WithdrawalStatus.SENDING);
                withdrawalRepository.save(w);
                audit(batch, w, WalletAuditEventType.PAYOUT_BATCH_APPROVED, actor,
                        "Approved by an Elimika admin in a payout batch", extra);
            }
            log.info("Payout batch {} released by {}: {} rows, {} (float {}, override {})", batch.getUuid(), actor,
                    batch.getRowCount(), batch.getTotalAmount(), utility, utility == null);
            return batch;
        });
    }

    @Override
    @Transactional
    public PayoutBatch reject(UUID batchUuid, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Say why the batch is rejected.");
        }
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.AWAITING_APPROVAL, "Only a batch waiting for approval can be rejected.");
        UUID actor = domainSecurityService.getCurrentUserUuid();
        batch.setStatus(PayoutBatchStatus.REJECTED);
        batch.setRejectedByUserUuid(actor);
        batch.setRejectedByName(actorName());
        batch.setRejectedAt(now());
        batch.setRejectionReason(reason.trim());
        release(batch, WalletAuditEventType.PAYOUT_BATCH_REJECTED, actor, "Payout batch rejected: " + reason.trim());
        return batchRepository.save(batch);
    }

    @Override
    @Transactional
    public PayoutBatch returnToDraft(UUID batchUuid) {
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.AWAITING_APPROVAL, "Only a batch waiting for approval can go back to draft.");
        UUID actor = domainSecurityService.getCurrentUserUuid();
        Map<String, Object> extra = new HashMap<>();
        if (batch.getSubmittedByUserUuid() != null) {
            extra.put("previous_submitted_by", batch.getSubmittedByUserUuid().toString());
        }
        batch.setStatus(PayoutBatchStatus.DRAFT);
        batch.setSubmittedByUserUuid(null);
        batch.setSubmittedByName(null);
        batch.setSubmittedAt(null);
        batchRepository.save(batch);
        withdrawalRepository.findByPayoutBatchUuid(batch.getUuid()).forEach(w -> audit(batch, w,
                WalletAuditEventType.PAYOUT_BATCH_RETURNED_TO_DRAFT, actor, "Payout batch returned to draft", extra));
        return batch;
    }

    @Override
    @Transactional
    public PayoutBatch cancel(UUID batchUuid) {
        PayoutBatch batch = locked(batchUuid);
        requireStatus(batch, PayoutBatchStatus.DRAFT, "Only a draft batch can be cancelled.");
        batch.setStatus(PayoutBatchStatus.CANCELLED);
        release(batch, WalletAuditEventType.PAYOUT_BATCH_CANCELLED, domainSecurityService.getCurrentUserUuid(),
                "Payout batch cancelled");
        return batchRepository.save(batch);
    }

    @Override
    @Transactional
    public Withdrawal submitSingle(UUID withdrawalUuid) {
        PayoutBatch batch = create(List.of(withdrawalUuid));
        submit(batch.getUuid());
        return withdrawalRepository.findByUuid(withdrawalUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found."));
    }

    @Override
    @Transactional
    public void settle(UUID batchUuid) {
        PayoutBatch batch = batchRepository.lockByUuid(batchUuid).orElse(null);
        if (batch == null || batch.getStatus() != PayoutBatchStatus.RELEASED) {
            return;
        }
        List<Withdrawal> rows = withdrawalRepository.findByPayoutBatchUuid(batchUuid);
        if (rows.stream().anyMatch(w -> IN_FLIGHT.contains(w.getStatus()) || w.getStatus() == WithdrawalStatus.NEEDS_REVIEW)) {
            return;
        }
        boolean allPaid = rows.stream().allMatch(w -> w.getStatus() == WithdrawalStatus.PAID);
        batch.setStatus(allPaid ? PayoutBatchStatus.COMPLETED : PayoutBatchStatus.PARTLY_FAILED);
        batch.setCompletedAt(now());
        batchRepository.save(batch);
        long paid = rows.stream().filter(w -> w.getStatus() == WithdrawalStatus.PAID).count();
        Map<String, Object> extra = Map.of("paid_count", paid, "failed_count", rows.size() - paid);
        rows.forEach(w -> audit(batch, w, WalletAuditEventType.PAYOUT_BATCH_COMPLETED, null,
                allPaid ? "Payout batch completed" : "Payout batch finished with failures", extra));
        log.info("Payout batch {} finished {}: {} of {} paid", batchUuid, batch.getStatus(), paid, rows.size());
    }

    private PayoutBatch add(PayoutBatch batch, Collection<UUID> withdrawalUuids, UUID actor) {
        if (withdrawalUuids == null || withdrawalUuids.isEmpty()) {
            return batch;
        }
        List<Withdrawal> added = new ArrayList<>();
        for (UUID uuid : new LinkedHashSet<>(withdrawalUuids)) {
            Withdrawal withdrawal = withdrawalRepository.lockByUuid(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Withdrawal " + uuid + " not found."));
            if (batch.getUuid().equals(withdrawal.getPayoutBatchUuid())) {
                continue;
            }
            if (withdrawal.getPayoutBatchUuid() != null) {
                throw new IllegalStateException("Withdrawal " + withdrawal.getReference() + " is already in another payout batch.");
            }
            if (withdrawal.getStatus() != WithdrawalStatus.NEEDS_REVIEW) {
                throw new IllegalStateException("Withdrawal " + withdrawal.getReference() + " is "
                        + label(withdrawal.getStatus()) + "; only withdrawals waiting for review can be batched.");
            }
            withdrawal.setPayoutBatchUuid(batch.getUuid());
            withdrawalRepository.save(withdrawal);
            added.add(withdrawal);
        }
        withdrawalRepository.flush();
        recount(batch);
        batchRepository.save(batch);
        added.forEach(w -> audit(batch, w, WalletAuditEventType.PAYOUT_BATCH_ROW_ADDED, actor,
                "Added to a payout batch", Map.of()));
        return batch;
    }

    /** Lets a rejected or cancelled batch's rows go back to the review queue, still held. */
    private void release(PayoutBatch batch, WalletAuditEventType type, UUID actor, String reason) {
        for (Withdrawal w : withdrawalRepository.lockByPayoutBatchUuid(batch.getUuid())) {
            w.setPayoutBatchUuid(null);
            withdrawalRepository.save(w);
            audit(batch, w, type, actor, reason, Map.of());
        }
    }

    private void recount(PayoutBatch batch) {
        Object[] totals = withdrawalRepository.batchTotals(batch.getUuid()).getFirst();
        batch.setRowCount(((Number) totals[0]).intValue());
        batch.setTotalAmount(totals[1] instanceof BigDecimal sum ? sum : new BigDecimal(totals[1].toString()));
    }

    private void requireApprovable(PayoutBatch batch, UUID actor) {
        requireStatus(batch, PayoutBatchStatus.AWAITING_APPROVAL, "Only a batch waiting for approval can be approved.");
        if (actor == null || actor.equals(batch.getCreatedByUserUuid()) || actor.equals(batch.getSubmittedByUserUuid())) {
            throw new IllegalStateException("A different admin must approve this batch: whoever built or submitted it cannot.");
        }
    }

    private Optional<PayoutFloat> readFloat() {
        try {
            return gateway.currentFloat();
        } catch (RuntimeException ex) {
            log.warn("Could not read the payout float for a batch approval: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private PayoutBatch locked(UUID batchUuid) {
        return batchRepository.lockByUuid(batchUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Payout batch not found."));
    }

    private static void requireStatus(PayoutBatch batch, PayoutBatchStatus expected, String message) {
        if (batch.getStatus() != expected) {
            throw new IllegalStateException(message + " This batch is " + label(batch.getStatus()) + ".");
        }
    }

    private String actorName() {
        return domainSecurityService.getCurrentCallerContact().map(DomainSecurityService.CallerContact::name).orElse(null);
    }

    private void audit(PayoutBatch batch, Withdrawal withdrawal, WalletAuditEventType type, UUID actor, String reason,
                       Map<String, Object> extra) {
        Map<String, Object> values = new HashMap<>(extra);
        values.put("batch_uuid", batch.getUuid().toString());
        values.put("batch_status", batch.getStatus().name());
        values.put("row_count", batch.getRowCount());
        values.put("total_amount", batch.getTotalAmount());
        if (batch.getFloatAtApproval() != null) {
            values.put("float_at_approval", batch.getFloatAtApproval());
        }
        values.put("reference", withdrawal.getReference());
        values.put("amount", withdrawal.getAmount());
        values.put("net_amount", withdrawal.getNetAmount());
        values.put("status", withdrawal.getStatus().name());
        values.put("msisdn", Msisdn.mask(withdrawal.getMsisdn()));
        WalletAuditEvent event = WalletAuditEvent.of(withdrawal.owner(), type, actor, withdrawal.getReference(), reason,
                Map.of(), values);
        event.setCurrencyCode(withdrawal.getCurrencyCode());
        auditRepository.save(event);
    }

    private static String label(Enum<?> status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
