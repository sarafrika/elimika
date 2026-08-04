package apps.sarafrika.elimika.wallet.ledger.impl;

import apps.sarafrika.elimika.wallet.entity.LedgerAccount;
import apps.sarafrika.elimika.wallet.entity.LedgerAccountBalance;
import apps.sarafrika.elimika.wallet.entity.LedgerTransaction;
import apps.sarafrika.elimika.wallet.enums.LedgerEntryDirection;
import apps.sarafrika.elimika.wallet.factory.LedgerFactory;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingLeg;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingRequest;
import apps.sarafrika.elimika.wallet.ledger.LedgerService;
import apps.sarafrika.elimika.wallet.repository.LedgerAccountBalanceRepository;
import apps.sarafrika.elimika.wallet.repository.LedgerAccountRepository;
import apps.sarafrika.elimika.wallet.repository.LedgerEntryRepository;
import apps.sarafrika.elimika.wallet.repository.LedgerTransactionRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts balanced transactions and keeps the cached balances in step.
 * <p>
 * Postings run in their own transaction ({@link Propagation#REQUIRES_NEW}) so a ledger failure -
 * including a constraint violation, which poisons whatever transaction it happens in - is contained
 * and cannot take a wallet operation down with it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerAccountBalanceRepository balanceRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UUID> post(LedgerPostingRequest request) {
        assertBalanced(request);

        // The database enforces this too (unique idempotency_key), but a duplicate replay is the
        // expected case rather than an error, so it is worth not throwing over.
        if (transactionRepository.existsByIdempotencyKey(request.idempotencyKey())) {
            log.debug("Ledger transaction {} already posted; skipping", request.idempotencyKey());
            return Optional.empty();
        }

        LedgerTransaction transaction = transactionRepository.save(LedgerFactory.toTransaction(request));
        UUID transactionUuid = transaction.getUuid();

        for (LedgerPostingLeg leg : request.legs()) {
            UUID accountUuid = getOrCreateAccountUuid(leg.account());
            entryRepository.save(LedgerFactory.toEntry(transactionUuid, accountUuid, leg));
            balanceRepository.applyPostedDelta(accountUuid, naturalDelta(leg));
        }

        // Push the inserts out now so anything wrong with them surfaces here. The zero-sum trigger
        // is deferred by design and still fires at commit.
        entryRepository.flush();
        return Optional.of(transactionUuid);
    }

    @Override
    @Transactional
    public UUID getOrCreateAccountUuid(LedgerAccountRef ref) {
        Optional<LedgerAccount> existing = accountRepository.findByIdentity(
                ref.ownerType(), ref.ownerUuid(), ref.purse(), ref.currencyCode());
        if (existing.isPresent()) {
            return existing.get().getUuid();
        }

        accountRepository.insertIfAbsent(
                ref.ownerType().name(),
                ref.ownerUuid(),
                ref.accountType().name(),
                ref.purse().name(),
                ref.currencyCode());

        return accountRepository.findByIdentity(ref.ownerType(), ref.ownerUuid(), ref.purse(), ref.currencyCode())
                .map(LedgerAccount::getUuid)
                .orElseThrow(() -> new IllegalStateException("Failed to provision ledger account " + ref));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal derivedBalance(UUID accountUuid) {
        BigDecimal derived = entryRepository.derivedBalance(accountUuid);
        return derived == null ? BigDecimal.ZERO : derived;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal cachedBalance(UUID accountUuid) {
        return balanceRepository.findByAccountUuid(accountUuid)
                .map(LedgerAccountBalance::getPostedAmount)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * The delta this leg applies to the account's cached balance, in the account's natural sign:
     * a debit increases an asset or expense account, a credit increases a liability or revenue one.
     */
    private BigDecimal naturalDelta(LedgerPostingLeg leg) {
        boolean increases =
                leg.account().accountType().isDebitNormal() == (leg.direction() == LedgerEntryDirection.DEBIT);
        return increases ? leg.amount() : leg.amount().negate();
    }

    /**
     * Defence in depth. The database is what actually guarantees this - see the deferred constraint
     * trigger in the ledger migration - but failing here gives a caller a usable message instead of
     * a trigger exception at commit time.
     */
    private void assertBalanced(LedgerPostingRequest request) {
        if (request.legs().size() < 2) {
            throw new IllegalArgumentException("A ledger transaction needs at least two entries");
        }
        Map<String, BigDecimal> netByCurrency = new LinkedHashMap<>();
        for (LedgerPostingLeg leg : request.legs()) {
            netByCurrency.merge(leg.account().currencyCode(), leg.signedAmount(), BigDecimal::add);
        }
        netByCurrency.forEach((currency, net) -> {
            if (net.signum() != 0) {
                throw new IllegalArgumentException(
                        "Ledger transaction does not balance in %s: debits minus credits = %s"
                                .formatted(currency, net.toPlainString()));
            }
        });
    }
}
