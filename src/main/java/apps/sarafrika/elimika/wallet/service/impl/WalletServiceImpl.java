package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingLeg;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingRequest;
import apps.sarafrika.elimika.wallet.ledger.LedgerService;
import apps.sarafrika.elimika.wallet.repository.UserWalletRepository;
import apps.sarafrika.elimika.wallet.repository.UserWalletTransactionRepository;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletTransferResult;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Wallet balances, plus a dual-write into the double-entry ledger.
 * <p>
 * {@code user_wallets} is still authoritative: it is the only thing read, the only thing a balance
 * check consults, and the only thing a transfer's sufficiency test looks at. Every mutation is also
 * mirrored into the ledger, but the ledger is written after the fact and read by nobody yet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String CAUSE_WALLET_TRANSACTION = "WALLET_TRANSACTION";
    private static final String CAUSE_WALLET_TRANSFER = "WALLET_TRANSFER";

    private final UserWalletRepository userWalletRepository;
    private final UserWalletTransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final LedgerService ledgerService;

    @Override
    @Transactional
    public UserWallet getOrCreateWallet(UUID userUuid, String currencyCode) {
        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        return lockOrCreateWallet(userUuid, resolvedCurrency);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserWalletTransaction> getTransactions(UUID userUuid, String currencyCode, Pageable pageable) {
        return userWalletRepository.findByUserUuidAndCurrencyCode(userUuid, resolveCurrencyCode(currencyCode))
                .map(wallet -> transactionRepository.findByWalletUuid(wallet.getUuid(), pageable))
                .orElseGet(() -> Page.empty(pageable));
    }

    @Override
    @Transactional
    public UserWallet deposit(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description) {
        return applyCredit(userUuid, amount, currencyCode, WalletTransactionType.DEPOSIT, reference, description, null);
    }

    @Override
    @Transactional
    public UserWallet creditSale(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description) {
        return applyCredit(userUuid, amount, currencyCode, WalletTransactionType.SALE, reference, description, null);
    }

    @Override
    @Transactional
    public boolean creditSaleIdempotent(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description) {
        if (StringUtils.hasText(reference) && transactionRepository.existsByReference(reference)) {
            log.debug("Skipping duplicate sale credit for reference {}", reference);
            return false;
        }
        try {
            creditSale(userUuid, amount, currencyCode, reference, description);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate delivery raced past the existsByReference check; the
            // unique index on (reference) for SALE transactions rejected the second insert.
            log.debug("Sale credit for reference {} already recorded concurrently; skipping", reference);
            return false;
        }
    }

    @Override
    @Transactional
    public WalletTransferResult transfer(UUID fromUserUuid, UUID toUserUuid, BigDecimal amount, String currencyCode, String reference, String description) {
        if (fromUserUuid == null || toUserUuid == null) {
            throw new IllegalArgumentException("Both source and target users are required");
        }
        if (fromUserUuid.equals(toUserUuid)) {
            throw new IllegalArgumentException("Source and target users must be different");
        }
        validateAmount(amount);

        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        List<UUID> ordered = List.of(fromUserUuid, toUserUuid).stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        UserWallet first = lockOrCreateWallet(ordered.get(0), resolvedCurrency);
        UserWallet second = lockOrCreateWallet(ordered.get(1), resolvedCurrency);
        UserWallet source = fromUserUuid.equals(first.getUserUuid()) ? first : second;
        UserWallet target = fromUserUuid.equals(first.getUserUuid()) ? second : first;

        UUID transferReference = UUID.randomUUID();
        applyDebit(source, amount, WalletTransactionType.TRANSFER_OUT, reference, description, toUserUuid, transferReference);
        applyCredit(target, amount, resolvedCurrency, WalletTransactionType.TRANSFER_IN, reference, description, fromUserUuid, transferReference);
        recordLedgerTransfer(source, target, amount, resolvedCurrency, description, transferReference);

        return new WalletTransferResult(source, target, transferReference);
    }

    private UserWallet applyCredit(
            UUID userUuid,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid
    ) {
        validateAmount(amount);
        String resolvedCurrency = resolveCurrencyCode(currencyCode);
        UserWallet wallet = lockOrCreateWallet(userUuid, resolvedCurrency);
        UserWalletTransaction transaction =
                applyCredit(wallet, amount, resolvedCurrency, type, reference, description, counterpartyUserUuid, null);
        recordLedgerCredit(wallet, transaction, amount, resolvedCurrency, type, description);
        return wallet;
    }

    private UserWalletTransaction applyCredit(
            UserWallet wallet,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        BigDecimal balanceBefore = wallet.getBalanceAmount();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        wallet.setBalanceAmount(balanceAfter);
        userWalletRepository.save(wallet);

        UserWalletTransaction transaction = buildTransaction(
                wallet,
                type,
                amount,
                currencyCode,
                balanceBefore,
                balanceAfter,
                reference,
                description,
                counterpartyUserUuid,
                transferReference
        );
        transactionRepository.save(transaction);
        return transaction;
    }

    private UserWalletTransaction applyDebit(
            UserWallet wallet,
            BigDecimal amount,
            WalletTransactionType type,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        validateAmount(amount);
        BigDecimal balanceBefore = wallet.getBalanceAmount();
        if (balanceBefore.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        wallet.setBalanceAmount(balanceAfter);
        userWalletRepository.save(wallet);

        UserWalletTransaction transaction = buildTransaction(
                wallet,
                type,
                amount,
                wallet.getCurrencyCode(),
                balanceBefore,
                balanceAfter,
                reference,
                description,
                counterpartyUserUuid,
                transferReference
        );
        transactionRepository.save(transaction);
        return transaction;
    }

    private UserWalletTransaction buildTransaction(
            UserWallet wallet,
            WalletTransactionType type,
            BigDecimal amount,
            String currencyCode,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String reference,
            String description,
            UUID counterpartyUserUuid,
            UUID transferReference
    ) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setReference(reference);
        transaction.setDescription(description);
        transaction.setCounterpartyUserUuid(counterpartyUserUuid);
        transaction.setTransferReference(transferReference);
        return transaction;
    }

    /**
     * Mirrors a wallet credit into the ledger: the earner's balance is a liability of the platform,
     * so a credit increases it and something has to fund it.
     * <p>
     * A deposit is funded by cash the platform actually received; a sale is funded out of revenue
     * that has come in but not yet been attributed to anybody. Which of the two applies is the only
     * choice made here, and it follows from the wallet transaction type alone - no rule is read, no
     * fee is resolved, nothing outside this module is consulted.
     */
    private void recordLedgerCredit(
            UserWallet wallet,
            UserWalletTransaction transaction,
            BigDecimal amount,
            String currencyCode,
            WalletTransactionType type,
            String description
    ) {
        LedgerAccountRef earner = LedgerAccountRef.userEarnings(wallet.getUserUuid(), currencyCode);
        LedgerAccountRef funding = type == WalletTransactionType.DEPOSIT
                ? LedgerAccountRef.platformCashMpesa(currencyCode)
                : LedgerAccountRef.platformUnallocatedRevenue(currencyCode);

        postToLedger(new LedgerPostingRequest(
                "wallet-txn:" + transaction.getUuid(),
                transaction.getCreatedDate(),
                description,
                CAUSE_WALLET_TRANSACTION,
                transaction.getUuid(),
                List.of(LedgerPostingLeg.debit(funding, amount), LedgerPostingLeg.credit(earner, amount))));
    }

    /**
     * A transfer is the one movement that needs no platform account at all: one party's liability
     * falls by exactly what the other's rises.
     */
    private void recordLedgerTransfer(
            UserWallet source,
            UserWallet target,
            BigDecimal amount,
            String currencyCode,
            String description,
            UUID transferReference
    ) {
        LedgerAccountRef from = LedgerAccountRef.userEarnings(source.getUserUuid(), currencyCode);
        LedgerAccountRef to = LedgerAccountRef.userEarnings(target.getUserUuid(), currencyCode);

        postToLedger(new LedgerPostingRequest(
                "wallet-transfer:" + transferReference,
                null,
                description,
                CAUSE_WALLET_TRANSFER,
                transferReference,
                List.of(LedgerPostingLeg.debit(from, amount), LedgerPostingLeg.credit(to, amount))));
    }

    /**
     * Posts after the wallet transaction has committed, never inside it.
     * <p>
     * This is the deliberate trade of the dual-write phase. Doing it inline would put the ledger's
     * constraints on the critical path of getting somebody paid - and a constraint violation marks
     * the whole transaction rollback-only, so even catching it would not save the credit. Posting
     * from {@code afterCommit} means a ledger failure cannot reach the wallet operation at all, at
     * the cost of the two writes not being atomic: a crash in the window between them, or a
     * genuinely rejected posting, leaves the ledger behind. That gap is exactly what
     * {@link apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob} exists to find.
     * <p>
     * When there is no transaction to hang off - which in practice means a unit test - the posting
     * happens inline instead, so the behaviour is still exercised.
     */
    private void postToLedger(LedgerPostingRequest request) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            postSwallowingFailures(request);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                postSwallowingFailures(request);
            }
        });
    }

    private void postSwallowingFailures(LedgerPostingRequest request) {
        try {
            ledgerService.post(request);
        } catch (Exception ex) {
            // Nothing above this can be allowed to fail because the ledger did. The wallet has
            // already committed and stays authoritative; reconciliation reports the divergence.
            log.error("Ledger posting {} failed; user_wallets is unchanged and reconciliation will report this: {}",
                    request.idempotencyKey(), ex.getMessage(), ex);
        }
    }

    private UserWallet lockOrCreateWallet(UUID userUuid, String currencyCode) {
        Optional<UserWallet> existing = userWalletRepository.findLockedByUserUuidAndCurrencyCode(userUuid, currencyCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserWallet wallet = new UserWallet();
        wallet.setUserUuid(userUuid);
        wallet.setCurrencyCode(currencyCode);
        wallet.setBalanceAmount(ZERO);

        try {
            userWalletRepository.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Wallet already created for user {} and currency {}", userUuid, currencyCode, ex);
        }

        return userWalletRepository.findLockedByUserUuidAndCurrencyCode(userUuid, currencyCode)
                .orElseThrow(() -> new IllegalStateException("Failed to create wallet for user"));
    }

    private String resolveCurrencyCode(String currencyCode) {
        PlatformCurrency currency = currencyService.resolveCurrencyOrDefault(currencyCode);
        return currency.getCode();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
