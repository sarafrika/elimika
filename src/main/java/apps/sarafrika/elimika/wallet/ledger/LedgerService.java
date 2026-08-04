package apps.sarafrika.elimika.wallet.ledger;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * The double-entry accounting engine.
 * <p>
 * It moves money between accounts and nothing else. It reads no rules, resolves no fees and knows
 * nothing about what a course or an organisation is: callers decide which accounts to touch and by
 * how much, and this decides only whether the result is valid accounting.
 * <p>
 * Nothing reads from the ledger yet. {@code user_wallets} remains authoritative for every balance
 * the platform shows or spends; the ledger is written alongside it so the two can be compared by
 * {@link WalletLedgerReconciliationJob}.
 */
public interface LedgerService {

    /**
     * Posts one balanced transaction.
     *
     * @return the new transaction's UUID, or empty when {@code idempotencyKey} was already posted
     * @throws IllegalArgumentException                                   when the legs do not net to zero per currency
     * @throws org.springframework.dao.DataIntegrityViolationException    when a concurrent caller
     *                                                                    wins the race on the same key
     */
    Optional<UUID> post(LedgerPostingRequest request);

    /**
     * Resolves an account, creating it on first use. Safe under concurrency: a losing race falls
     * back to reading the row the winner inserted.
     */
    UUID getOrCreateAccountUuid(LedgerAccountRef ref);

    /** Recomputed from the entries. The truth. */
    BigDecimal derivedBalance(UUID accountUuid);

    /** Read from the cache. Should always equal {@link #derivedBalance(UUID)}. */
    BigDecimal cachedBalance(UUID accountUuid);
}
