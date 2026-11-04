package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Wallet balances per owner and purse. A person holds a COURSE_CREATOR, an INSTRUCTOR and a STUDENT purse, each with
 * its own balance and wallet row; an organisation holds EARNINGS. Reads without a purse sum the owner's purses.
 */
public interface WalletService {

    /** Every purse the owner can hold, in display order, read now; never creates a row. */
    List<WalletPurseBalance> pursesOf(WalletOwner owner, String currencyCode);

    /** Reads the balance without locking or creating a wallet; a missing wallet reads as zero. */
    WalletBalanceSummary getBalanceSummary(UUID userUuid, String currencyCode);

    /** The owner's balance summed over their purses; {@code wallet_uuid} is the owner's oldest purse row. */
    WalletBalanceSummary balanceOf(WalletOwner owner, String currencyCode);

    /** One purse's balance; a purse with no row yet reads as zero. */
    WalletBalanceSummary balanceOf(WalletOwner owner, WalletPurse purse, String currencyCode);

    /** Lifetime totals over every purse; moves between the owner's own purses are not counted as sent out. */
    WalletTotals totalsOf(WalletOwner owner, String currencyCode);

    Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode, Pageable pageable);

    Page<UserWalletTransaction> statementOf(WalletOwner owner, String currencyCode, WalletTransactionFilter filter,
                                            Pageable pageable);

    /** Every matching row, newest first, for a download; refuses more than {@code MAX_EXPORT_ROWS}. */
    List<UserWalletTransaction> exportOf(WalletOwner owner, String currencyCode, WalletTransactionFilter filter);

    int MAX_EXPORT_ROWS = 20_000;

    /** The wallet's audit trail, newest first. */
    Page<WalletAuditEvent> auditOf(WalletOwner owner, Pageable pageable);

    /** The last {@code months} calendar months (UTC), newest first, over every purse. */
    List<WalletMonthStatement> monthlyStatementsOf(WalletOwner owner, String currencyCode, int months);

    /** The same for one purse, or every purse when {@code purse} is null. */
    List<WalletMonthStatement> monthlyStatementsOf(WalletOwner owner, WalletPurse purse, String currencyCode,
                                                   int months);

    /** What is held for payouts in flight and what is free to use, over every purse. */
    WalletStanding standingOf(WalletOwner owner, String currencyCode, boolean canWithdraw);

    /**
     * The purse a debit comes from when the caller did not say: an organisation's EARNINGS; a person's only funded
     * purse. Refuses when a person has more than one funded purse, naming them, and checks an explicit purse fits
     * the owner.
     */
    WalletPurse debitPurse(WalletOwner owner, WalletPurse requested, String currencyCode);

    /**
     * Pays one earner's sale share into {@code purse}: a SALE credit of {@code gross}, then a PLATFORM_FEE debit
     * from the same purse when the fee is above zero, in one transaction. Returns false when {@code reference} was
     * already settled.
     */
    boolean settleSaleShare(WalletOwner owner, WalletPurse purse, BigDecimal gross, BigDecimal platformFee,
                            String currencyCode, String reference, String description);

    /**
     * Takes back a settled sale share from the purse it was paid into: a SALE_REVERSAL of what the share left
     * there (its gross less its platform fee), with the fee returned to the platform's unallocated revenue in the
     * ledger. Refuses when the purse no longer holds it; returns null when the share was already reversed.
     */
    UserWalletTransaction reverseSaleShare(String saleReference, String description);

    /** Takes a confirmed withdrawal's net amount out of the purse into money in flight. */
    UserWalletTransaction holdWithdrawal(WalletOwner owner, WalletPurse purse, BigDecimal netAmount,
                                         String currencyCode, String reference, String description);

    /** Charges the earner the M-Pesa fee on a withdrawal from the same purse, when they pay it. */
    UserWalletTransaction chargePayoutFee(WalletOwner owner, WalletPurse purse, BigDecimal fee, String currencyCode,
                                          String reference, String description);

    /**
     * Puts a withdrawal's net amount and any fee back where they were taken from: each purse its withdrawal and
     * charge rows debited, or {@code purse} when there are none.
     */
    List<UserWalletTransaction> reverseWithdrawal(WalletOwner owner, WalletPurse purse, BigDecimal netAmount,
                                                  BigDecimal fee, String currencyCode, String reference,
                                                  String description);

    /** Records in the ledger that M-Pesa paid a withdrawal out, with what M-Pesa charged the platform. */
    void settleWithdrawal(BigDecimal netAmount, BigDecimal mpesaCharges, String currencyCode, String reference,
                          String description);

    /** Credits a person's purse with money received; {@code purse} defaults to STUDENT. */
    UserWallet deposit(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode, String reference,
                       String description);

    UserWallet creditSale(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode, String reference,
                          String description);

    /**
     * Credits a sale into the earner's purse only when no prior transaction exists for the given reference.
     *
     * @return {@code true} when a credit was applied, {@code false} when skipped as a duplicate
     */
    boolean creditSaleIdempotent(UUID userUuid, WalletPurse purse, BigDecimal amount, String currencyCode,
                                 String reference, String description);

    /**
     * Sends money from one of a person's purses ({@link #debitPurse} when null) to another person's STUDENT purse.
     */
    WalletTransferResult transfer(UUID fromUserUuid, WalletPurse sourcePurse, UUID toUserUuid, BigDecimal amount,
                                  String currencyCode, String reference, String description);

    /** Moves money between two purses of one owner: a TRANSFER_OUT and a TRANSFER_IN row, no fee. */
    PurseTransferResult transferBetweenPurses(WalletOwner owner, WalletPurse from, WalletPurse to, BigDecimal amount,
                                              String currencyCode, String reference, String description);
}
