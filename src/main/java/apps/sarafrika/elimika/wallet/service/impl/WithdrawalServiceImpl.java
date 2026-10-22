package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.otp.IssuedCode;
import apps.sarafrika.elimika.shared.otp.OneTimeCodePurpose;
import apps.sarafrika.elimika.shared.otp.OneTimeCodeService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutOutcome;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.entity.PayoutSettings;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.PayoutMethodRepository;
import apps.sarafrika.elimika.wallet.repository.WalletAuditEventRepository;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WithdrawalNotifier;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WithdrawalServiceImpl implements WithdrawalService {

    static final String FLAG_ABOVE_REVIEW_LIMIT = "ABOVE_REVIEW_LIMIT";
    static final String FLAG_FIRST_PAYOUT = "FIRST_PAYOUT_TO_NUMBER";
    static final String FLAG_NAME_MISMATCH = "NAME_MISMATCH";

    private final WithdrawalRepository withdrawalRepository;
    private final PayoutMethodRepository methodRepository;
    private final WalletAuditEventRepository auditRepository;
    private final WalletService walletService;
    private final PayoutSettingsService settingsService;
    private final OneTimeCodeService oneTimeCodeService;
    private final DomainSecurityService domainSecurityService;
    private final MobileMoneyPayoutGateway gateway;
    private final WithdrawalNotifier notifier;
    private final TransactionTemplate transactions;
    private final Duration confirmationWindow;

    public WithdrawalServiceImpl(WithdrawalRepository withdrawalRepository, PayoutMethodRepository methodRepository,
                                 WalletAuditEventRepository auditRepository, WalletService walletService,
                                 PayoutSettingsService settingsService, OneTimeCodeService oneTimeCodeService,
                                 DomainSecurityService domainSecurityService, MobileMoneyPayoutGateway gateway,
                                 WithdrawalNotifier notifier, PlatformTransactionManager transactionManager,
                                 @Value("${wallet.withdrawals.confirmation-window:PT15M}") Duration confirmationWindow) {
        this.withdrawalRepository = withdrawalRepository;
        this.methodRepository = methodRepository;
        this.auditRepository = auditRepository;
        this.walletService = walletService;
        this.settingsService = settingsService;
        this.oneTimeCodeService = oneTimeCodeService;
        this.domainSecurityService = domainSecurityService;
        this.gateway = gateway;
        this.notifier = notifier;
        // Always its own transaction: these steps also run after another transaction has committed.
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.confirmationWindow = confirmationWindow;
    }

    @Override
    @Transactional
    public RequestedWithdrawal request(WalletOwner owner, BigDecimal amount, UUID payoutMethodUuid, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IllegalArgumentException("An idempotency key is required.");
        }
        Optional<Withdrawal> repeat = withdrawalRepository.findByOwnerTypeAndOwnerUuidAndIdempotencyKey(
                owner.type(), owner.uuid(), idempotencyKey);
        if (repeat.isPresent()) {
            return new RequestedWithdrawal(repeat.get(), null);
        }
        PayoutSettings settings = settingsService.current();
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Enter a whole number of shillings.");
        }
        if (amount.compareTo(settings.getMinAmount()) < 0) {
            throw new IllegalArgumentException("The minimum withdrawal is " + kes(settings.getMinAmount()) + ".");
        }
        if (amount.compareTo(settings.getMaxAmount()) > 0) {
            throw new IllegalArgumentException("The most you can withdraw at once is " + kes(settings.getMaxAmount()) + ".");
        }
        BigDecimal fee = settings.getFeePayer() == PayoutFeePayer.EARNER ? settings.getEarnerFee() : BigDecimal.ZERO;
        if (amount.compareTo(fee) <= 0) {
            throw new IllegalArgumentException("The amount must be more than the M-Pesa charge of " + kes(fee) + ".");
        }
        PayoutMethod method = usableMethod(owner, payoutMethodUuid);
        String currency = walletService.balanceOf(owner, null).currencyCode();
        BigDecimal available = walletService.balanceOf(owner, currency).balanceAmount();
        if (amount.compareTo(available) > 0) {
            throw new IllegalArgumentException("You can withdraw up to " + kes(available) + ".");
        }
        BigDecimal today = withdrawalRepository.sumSince(owner.type(), owner.uuid(), WithdrawalStatus.COUNTED,
                LocalDate.now(ZoneOffset.UTC).atStartOfDay());
        if (today.add(amount).compareTo(settings.getDailyLimit()) > 0) {
            throw new IllegalArgumentException("This would pass today's limit of " + kes(settings.getDailyLimit())
                    + ". You can still withdraw " + kes(settings.getDailyLimit().subtract(today).max(BigDecimal.ZERO)) + " today.");
        }

        UUID actor = domainSecurityService.getCurrentUserUuid();
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setReference("WD-" + Year.now(ZoneOffset.UTC) + "-"
                + String.format("%06d", withdrawalRepository.nextReferenceNumber()));
        withdrawal.setOwnerType(owner.type());
        withdrawal.setOwnerUuid(owner.uuid());
        withdrawal.setCurrencyCode(currency);
        withdrawal.setAmount(amount);
        withdrawal.setFeeAmount(fee);
        withdrawal.setNetAmount(amount.subtract(fee));
        withdrawal.setPayoutMethodUuid(method.getUuid());
        withdrawal.setMsisdn(method.getMsisdn());
        withdrawal.setStatus(WithdrawalStatus.AWAITING_CONFIRMATION);
        withdrawal.setIdempotencyKey(idempotencyKey);
        withdrawal.setRequestedByUserUuid(actor);
        withdrawal.setRequesterName(domainSecurityService.getCurrentCallerContact()
                .map(DomainSecurityService.CallerContact::name).orElse(null));
        try {
            withdrawalRepository.saveAndFlush(withdrawal);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A withdrawal to this number is already in progress.");
        }
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_REQUESTED, actor, "Withdrawal requested", Map.of());
        return new RequestedWithdrawal(withdrawal, sendCode(withdrawal, actor));
    }

    @Override
    @Transactional
    public IssuedCode resendCode(WalletOwner owner, UUID withdrawalUuid) {
        Withdrawal withdrawal = find(owner, withdrawalUuid);
        requireStatus(withdrawal, WithdrawalStatus.AWAITING_CONFIRMATION);
        return sendCode(withdrawal, domainSecurityService.getCurrentUserUuid());
    }

    @Override
    @Transactional
    public Withdrawal confirm(WalletOwner owner, UUID withdrawalUuid, String code) {
        Withdrawal withdrawal = locked(owner, withdrawalUuid);
        requireStatus(withdrawal, WithdrawalStatus.AWAITING_CONFIRMATION);
        if (isPastConfirmationWindow(withdrawal)) {
            throw new IllegalArgumentException("This withdrawal has expired. Start a new one.");
        }
        oneTimeCodeService.verify(OneTimeCodePurpose.WITHDRAWAL_CONFIRMATION, withdrawal.getUuid(), code);
        PayoutMethod method = usableMethod(owner, withdrawal.getPayoutMethodUuid());
        BigDecimal available = walletService.balanceOf(owner, withdrawal.getCurrencyCode()).balanceAmount();
        if (withdrawal.getAmount().compareTo(available) > 0) {
            throw new IllegalArgumentException("Your balance changed; you can withdraw up to " + kes(available) + ".");
        }

        String description = "Withdrawal to M-Pesa " + Msisdn.mask(withdrawal.getMsisdn());
        walletService.holdWithdrawal(owner, withdrawal.getNetAmount(), withdrawal.getCurrencyCode(),
                withdrawal.getReference(), description);
        if (withdrawal.getFeeAmount().signum() > 0) {
            walletService.chargePayoutFee(owner, withdrawal.getFeeAmount(), withdrawal.getCurrencyCode(),
                    withdrawal.getReference(), "M-Pesa charge - " + description);
        }

        PayoutSettings settings = settingsService.current();
        List<String> flags = new ArrayList<>();
        if (withdrawal.getAmount().compareTo(settings.getReviewLimit()) > 0) {
            flags.add(FLAG_ABOVE_REVIEW_LIMIT);
        }
        if (!withdrawalRepository.existsByPayoutMethodUuidAndStatus(method.getUuid(), WithdrawalStatus.PAID)) {
            flags.add(FLAG_FIRST_PAYOUT);
        }
        if (method.isNameMismatch()) {
            flags.add(FLAG_NAME_MISMATCH);
        }
        withdrawal.setReviewFlags(flags.isEmpty() ? null : String.join(",", flags));
        withdrawal.setConfirmedAt(now());
        boolean secondApproval = owner.type() == WalletOwnerType.ORGANISATION
                && withdrawal.getAmount().compareTo(settings.getSecondApprovalLimit()) > 0;
        withdrawal.setStatus(secondApproval ? WithdrawalStatus.AWAITING_SECOND_APPROVAL
                : flags.isEmpty() ? WithdrawalStatus.SENDING : WithdrawalStatus.NEEDS_REVIEW);
        withdrawalRepository.save(withdrawal);
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_CONFIRMED, domainSecurityService.getCurrentUserUuid(),
                "Confirmed by SMS code", Map.of("status", withdrawal.getStatus().name(), "review_flags", flags));
        sendAfterCommit(withdrawal);
        return withdrawal;
    }

    @Override
    @Transactional
    public Withdrawal cancel(WalletOwner owner, UUID withdrawalUuid) {
        Withdrawal withdrawal = locked(owner, withdrawalUuid);
        UUID actor = domainSecurityService.getCurrentUserUuid();
        if (withdrawal.getStatus() == WithdrawalStatus.AWAITING_CONFIRMATION) {
            withdrawal.setStatus(WithdrawalStatus.CANCELLED);
            withdrawal.setCompletedAt(now());
        } else if (withdrawal.getStatus() == WithdrawalStatus.AWAITING_SECOND_APPROVAL) {
            reverse(withdrawal, "Cancelled before the second approval");
            withdrawal.setStatus(WithdrawalStatus.CANCELLED);
        } else {
            throw new IllegalArgumentException("This withdrawal can no longer be cancelled.");
        }
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_CANCELLED, actor, "Withdrawal cancelled", Map.of());
        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public Withdrawal approveAsSecond(WalletOwner owner, UUID withdrawalUuid) {
        Withdrawal withdrawal = locked(owner, withdrawalUuid);
        requireStatus(withdrawal, WithdrawalStatus.AWAITING_SECOND_APPROVAL);
        UUID actor = domainSecurityService.getCurrentUserUuid();
        if (actor == null || actor.equals(withdrawal.getRequestedByUserUuid())) {
            throw new IllegalArgumentException("A different payout approver must approve this withdrawal.");
        }
        withdrawal.setSecondApproverUserUuid(actor);
        withdrawal.setSecondApprovedAt(now());
        withdrawal.setStatus(withdrawal.reviewFlagList().isEmpty() ? WithdrawalStatus.SENDING : WithdrawalStatus.NEEDS_REVIEW);
        withdrawalRepository.save(withdrawal);
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_SECOND_APPROVED, actor, "Second approver agreed",
                Map.of("status", withdrawal.getStatus().name()));
        sendAfterCommit(withdrawal);
        return withdrawal;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Withdrawal> list(WalletOwner owner, Collection<WithdrawalStatus> statuses, Pageable pageable) {
        Collection<WithdrawalStatus> wanted = statuses == null || statuses.isEmpty()
                ? Arrays.asList(WithdrawalStatus.values()) : statuses;
        return withdrawalRepository.findByOwner(owner.type(), owner.uuid(), wanted, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Withdrawal get(WalletOwner owner, UUID withdrawalUuid) {
        return find(owner, withdrawalUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Withdrawal> queue(Collection<WithdrawalStatus> statuses, Pageable pageable) {
        Collection<WithdrawalStatus> wanted = statuses == null || statuses.isEmpty()
                ? Arrays.asList(WithdrawalStatus.values()) : statuses;
        return withdrawalRepository.findByStatusIn(wanted, pageable);
    }

    @Override
    @Transactional
    public Withdrawal adminApprove(UUID withdrawalUuid) {
        Withdrawal withdrawal = lockedAny(withdrawalUuid);
        requireStatus(withdrawal, WithdrawalStatus.NEEDS_REVIEW);
        UUID actor = domainSecurityService.getCurrentUserUuid();
        withdrawal.setReviewedByUserUuid(actor);
        withdrawal.setReviewedAt(now());
        withdrawal.setStatus(WithdrawalStatus.SENDING);
        withdrawalRepository.save(withdrawal);
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_APPROVED, actor, "Approved by an Elimika admin", Map.of());
        sendAfterCommit(withdrawal);
        return withdrawal;
    }

    @Override
    @Transactional
    public Withdrawal adminReject(UUID withdrawalUuid, String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() < 10) {
            throw new IllegalArgumentException("Tell the earner why, in a sentence.");
        }
        Withdrawal withdrawal = lockedAny(withdrawalUuid);
        if (withdrawal.getStatus() != WithdrawalStatus.NEEDS_REVIEW
                && withdrawal.getStatus() != WithdrawalStatus.AWAITING_SECOND_APPROVAL) {
            throw new IllegalArgumentException("Only a withdrawal waiting for approval can be rejected.");
        }
        UUID actor = domainSecurityService.getCurrentUserUuid();
        withdrawal.setReviewedByUserUuid(actor);
        withdrawal.setReviewedAt(now());
        withdrawal.setRejectionReason(reason.trim());
        reverse(withdrawal, "Rejected: " + reason.trim());
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawalRepository.save(withdrawal);
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_REJECTED, actor, reason.trim(), Map.of());
        afterCommit(() -> notifier.rejected(withdrawal));
        return withdrawal;
    }

    @Override
    public void send(UUID withdrawalUuid) {
        Withdrawal withdrawal = withdrawalRepository.findByUuid(withdrawalUuid).orElse(null);
        if (withdrawal == null || withdrawal.getStatus() != WithdrawalStatus.SENDING) {
            return;
        }
        PayoutOutcome outcome;
        try {
            outcome = gateway.submit(withdrawal.getUuid().toString(), withdrawal.getMsisdn(), withdrawal.getNetAmount(),
                    "Elimika withdrawal " + withdrawal.getReference());
        } catch (RuntimeException ex) {
            // Unknown whether it reached M-Pesa; the reference makes the next attempt safe.
            log.warn("Withdrawal {} not handed to M-Pesa yet: {}", withdrawal.getReference(), ex.getMessage());
            touch(withdrawalUuid);
            return;
        }
        apply(withdrawalUuid, outcome, WithdrawalStatus.SENDING);
    }

    @Override
    public void refresh(UUID withdrawalUuid) {
        Withdrawal withdrawal = withdrawalRepository.findByUuid(withdrawalUuid).orElse(null);
        if (withdrawal == null || withdrawal.getStatus() != WithdrawalStatus.PROCESSING) {
            return;
        }
        Optional<PayoutOutcome> outcome;
        try {
            outcome = gateway.status(withdrawal.getUuid().toString());
        } catch (RuntimeException ex) {
            log.warn("Could not read withdrawal {} from M-Pesa: {}", withdrawal.getReference(), ex.getMessage());
            touch(withdrawalUuid);
            return;
        }
        if (outcome.isEmpty()) {
            // The gateway never saw it: hand it over again under the same reference.
            transactions.executeWithoutResult(status -> withdrawalRepository.lockByUuid(withdrawalUuid)
                    .filter(w -> w.getStatus() == WithdrawalStatus.PROCESSING)
                    .ifPresent(w -> {
                        w.setStatus(WithdrawalStatus.SENDING);
                        withdrawalRepository.save(w);
                    }));
            return;
        }
        apply(withdrawalUuid, outcome.get(), WithdrawalStatus.PROCESSING);
    }

    @Override
    @Transactional
    public void expire(UUID withdrawalUuid) {
        withdrawalRepository.lockByUuid(withdrawalUuid)
                .filter(w -> w.getStatus() == WithdrawalStatus.AWAITING_CONFIRMATION && isPastConfirmationWindow(w))
                .ifPresent(w -> {
                    w.setStatus(WithdrawalStatus.EXPIRED);
                    w.setCompletedAt(now());
                    withdrawalRepository.save(w);
                });
    }

    private void apply(UUID withdrawalUuid, PayoutOutcome outcome, WithdrawalStatus expected) {
        transactions.executeWithoutResult(status -> withdrawalRepository.lockByUuid(withdrawalUuid)
                .filter(w -> w.getStatus() == expected)
                .ifPresent(w -> {
                    w.setLastCheckedAt(now());
                    w.setStatusChecks(w.getStatusChecks() + 1);
                    if (outcome.gatewayPayoutUuid() != null) {
                        w.setGatewayPayoutUuid(outcome.gatewayPayoutUuid());
                    }
                    switch (outcome.state()) {
                        case PENDING -> {
                            if (w.getSentAt() == null) {
                                w.setSentAt(now());
                            }
                            w.setStatus(WithdrawalStatus.PROCESSING);
                            withdrawalRepository.save(w);
                        }
                        case SUCCESS -> paid(w, outcome);
                        case FAILED -> failed(w, outcome.failureReason());
                    }
                }));
    }

    private void paid(Withdrawal withdrawal, PayoutOutcome outcome) {
        withdrawal.setStatus(WithdrawalStatus.PAID);
        withdrawal.setMpesaReceipt(outcome.receipt());
        withdrawal.setMpesaRecipientName(outcome.recipientName());
        withdrawal.setMpesaCharges(outcome.chargesPaid());
        withdrawal.setCompletedAt(now());
        if (withdrawal.getSentAt() == null) {
            withdrawal.setSentAt(withdrawal.getCompletedAt());
        }
        withdrawalRepository.save(withdrawal);
        walletService.settleWithdrawal(withdrawal.getNetAmount(), outcome.chargesPaid(), withdrawal.getCurrencyCode(),
                withdrawal.getReference(), "Withdrawal paid - " + withdrawal.getReference());
        recordRecipientName(withdrawal, outcome.recipientName());
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_PAID, null, "Paid by M-Pesa",
                outcome.receipt() == null ? Map.of() : Map.of("mpesa_receipt", outcome.receipt()));
        afterCommit(() -> notifier.paid(withdrawal));
    }

    private void failed(Withdrawal withdrawal, String reason) {
        String why = StringUtils.hasText(reason) ? reason : "M-Pesa could not complete the payout";
        withdrawal.setFailureReason(why);
        reverse(withdrawal, "M-Pesa failed the payout: " + why);
        withdrawal.setStatus(WithdrawalStatus.FAILED);
        withdrawalRepository.save(withdrawal);
        audit(withdrawal, WalletAuditEventType.WITHDRAWAL_FAILED, null, why, Map.of());
        afterCommit(() -> notifier.failed(withdrawal));
    }

    /** Puts the held money back; the withdrawal is finished either way. */
    private void reverse(Withdrawal withdrawal, String description) {
        walletService.reverseWithdrawal(withdrawal.owner(), withdrawal.getNetAmount(), withdrawal.getFeeAmount(),
                withdrawal.getCurrencyCode(), withdrawal.getReference(), "Withdrawal returned - " + description);
        withdrawal.setCompletedAt(now());
    }

    /** Keeps the name M-Pesa reports for the number, and flags it when it is not the person's own. */
    private void recordRecipientName(Withdrawal withdrawal, String recipientName) {
        if (!StringUtils.hasText(recipientName)) {
            return;
        }
        methodRepository.findByUuidAndOwnerTypeAndOwnerUuid(withdrawal.getPayoutMethodUuid(), withdrawal.getOwnerType(),
                withdrawal.getOwnerUuid()).ifPresent(method -> {
            if (method.getMpesaRegisteredName() == null) {
                method.setMpesaRegisteredName(recipientName);
            }
            if (withdrawal.getOwnerType() == WalletOwnerType.USER && !namesMatch(withdrawal.getRequesterName(), recipientName)) {
                method.setNameMismatch(true);
            }
            methodRepository.save(method);
        });
    }

    /** At least two words in common, or every word of the shorter name when it has fewer. */
    static boolean namesMatch(String profileName, String mpesaName) {
        if (!StringUtils.hasText(profileName) || !StringUtils.hasText(mpesaName)) {
            return true;
        }
        Set<String> profile = words(profileName);
        Set<String> mpesa = words(mpesaName);
        long common = profile.stream().filter(mpesa::contains).count();
        return common >= Math.min(2, Math.min(profile.size(), mpesa.size()));
    }

    private static Set<String> words(String name) {
        return Arrays.stream(name.toUpperCase(Locale.ROOT).split("[^\\p{L}]+"))
                .filter(word -> word.length() >= 2)
                .collect(Collectors.toSet());
    }

    private IssuedCode sendCode(Withdrawal withdrawal, UUID actor) {
        String amount = kes(withdrawal.getNetAmount());
        return oneTimeCodeService.issue(OneTimeCodePurpose.WITHDRAWAL_CONFIRMATION, withdrawal.getUuid(), actor,
                withdrawal.getMsisdn(), code -> "Your Elimika code to withdraw " + amount + " to this number is " + code
                        + ". It expires in 10 minutes. Never share it with anyone.");
    }

    private PayoutMethod usableMethod(WalletOwner owner, UUID methodUuid) {
        PayoutMethod method = methodRepository.findByUuidAndOwnerTypeAndOwnerUuid(methodUuid, owner.type(), owner.uuid())
                .filter(m -> m.getStatus() == PayoutMethodStatus.VERIFIED)
                .orElseThrow(() -> new IllegalArgumentException("Choose a verified payout number."));
        if (!method.isUsable(now())) {
            throw new IllegalArgumentException("This number was added recently and is on hold until "
                    + method.getUnlockAt().atZoneSameInstant(java.time.ZoneId.of("Africa/Nairobi"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)) + " (EAT).");
        }
        return method;
    }

    private Withdrawal find(WalletOwner owner, UUID withdrawalUuid) {
        return withdrawalRepository.findByUuidAndOwnerTypeAndOwnerUuid(withdrawalUuid, owner.type(), owner.uuid())
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found."));
    }

    private Withdrawal locked(WalletOwner owner, UUID withdrawalUuid) {
        Withdrawal withdrawal = lockedAny(withdrawalUuid);
        if (withdrawal.getOwnerType() != owner.type() || !withdrawal.getOwnerUuid().equals(owner.uuid())) {
            throw new ResourceNotFoundException("Withdrawal not found.");
        }
        return withdrawal;
    }

    private Withdrawal lockedAny(UUID withdrawalUuid) {
        return withdrawalRepository.lockByUuid(withdrawalUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found."));
    }

    private static void requireStatus(Withdrawal withdrawal, WithdrawalStatus expected) {
        if (withdrawal.getStatus() != expected) {
            throw new IllegalArgumentException("This withdrawal is " + withdrawal.getStatus().name().toLowerCase(Locale.ROOT)
                    .replace('_', ' ') + " and cannot do that now.");
        }
    }

    private boolean isPastConfirmationWindow(Withdrawal withdrawal) {
        return withdrawal.getCreatedDate() != null
                && withdrawal.getCreatedDate().atOffset(ZoneOffset.UTC).plus(confirmationWindow).isBefore(now());
    }

    private void touch(UUID withdrawalUuid) {
        transactions.executeWithoutResult(status -> withdrawalRepository.lockByUuid(withdrawalUuid).ifPresent(w -> {
            w.setLastCheckedAt(now());
            w.setStatusChecks(w.getStatusChecks() + 1);
            withdrawalRepository.save(w);
        }));
    }

    private void sendAfterCommit(Withdrawal withdrawal) {
        if (withdrawal.getStatus() == WithdrawalStatus.SENDING) {
            UUID uuid = withdrawal.getUuid();
            afterCommit(() -> send(uuid));
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    log.error("After-commit withdrawal step failed; the processor will retry: {}", ex.getMessage(), ex);
                }
            }
        });
    }

    private void audit(Withdrawal withdrawal, WalletAuditEventType type, UUID actor, String reason,
                       Map<String, Object> extra) {
        Map<String, Object> values = new HashMap<>(extra);
        values.put("reference", withdrawal.getReference());
        values.put("amount", withdrawal.getAmount());
        values.put("status", withdrawal.getStatus().name());
        values.put("msisdn", Msisdn.mask(withdrawal.getMsisdn()));
        WalletAuditEvent event = WalletAuditEvent.of(withdrawal.owner(), type, actor, withdrawal.getReference(), reason,
                Map.of(), values);
        event.setCurrencyCode(withdrawal.getCurrencyCode());
        auditRepository.save(event);
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    static String kes(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return "KES " + format.format(amount);
    }
}
