package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    UserWallet getOrCreateWallet(UUID userUuid, String currencyCode);

    /** Reads the balance without locking or creating a wallet; a missing wallet reads as zero. */
    WalletBalanceSummary getBalanceSummary(UUID userUuid, String currencyCode);

    Page<UserWalletTransaction> getTransactions(UUID userUuid, String currencyCode, Pageable pageable);

    /** Reads any owner's balance without locking or creating a wallet; a missing wallet reads as zero. */
    WalletBalanceSummary balanceOf(WalletOwner owner, String currencyCode);

    /** Lifetime totals for a wallet; zero when the owner has no wallet yet. */
    WalletTotals totalsOf(WalletOwner owner, String currencyCode);

    Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode, Pageable pageable);

    Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode, WalletTransactionFilter filter,
                                            Pageable pageable);

    /** Every matching row, newest first, for a download; refuses more than {@code MAX_EXPORT_ROWS}. */
    List<UserWalletTransaction> exportOf(WalletOwner owner, String currencyCode, WalletTransactionFilter filter);

    int MAX_EXPORT_ROWS = 20_000;

    /** The wallet's audit trail, newest first. */
    Page<WalletAuditEvent> auditOf(WalletOwner owner, Pageable pageable);

    /** The last {@code months} calendar months (UTC), newest first; quiet months carry the balance over. */
    List<WalletMonthStatement> monthlyStatementsOf(WalletOwner owner, String currencyCode, int months);

    /** What is held for payouts in flight and what is free to use; the caller decides {@code canWithdraw}. */
    WalletStanding standingOf(WalletOwner owner, String currencyCode, boolean canWithdraw);

    /**
     * Pays one earner's sale share: a SALE credit of {@code gross}, then a PLATFORM_FEE debit when the fee
     * is above zero, in one transaction. Returns false when {@code reference} was already settled.
     */
    boolean settleSaleShare(WalletOwner owner, BigDecimal gross, BigDecimal platformFee, String currencyCode,
                            String reference, String description);

    UserWallet deposit(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description);

    UserWallet creditSale(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description);

    /**
     * Credits a sale into the earner's wallet only when no prior transaction exists for the given
     * reference. Provides idempotency for event-driven crediting (e.g. order capture replays).
     *
     * @return {@code true} when a credit was applied, {@code false} when skipped as a duplicate
     */
    boolean creditSaleIdempotent(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description);

    WalletTransferResult transfer(UUID fromUserUuid, UUID toUserUuid, BigDecimal amount, String currencyCode, String reference, String description);
}
