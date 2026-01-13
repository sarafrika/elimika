package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final UserWalletRepository userWalletRepository;
    private final UserWalletTransactionRepository transactionRepository;
    private final CurrencyService currencyService;

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
        applyCredit(wallet, amount, resolvedCurrency, type, reference, description, counterpartyUserUuid, null);
        return wallet;
    }

    private void applyCredit(
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
    }

    private void applyDebit(
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
