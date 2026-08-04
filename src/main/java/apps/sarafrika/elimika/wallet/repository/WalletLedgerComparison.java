package apps.sarafrika.elimika.wallet.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One wallet placed next to its derived ledger balance. Projection for
 * {@link UserWalletRepository#compareWalletsAgainstLedger(long, int)}.
 */
public interface WalletLedgerComparison {

    /** Cursor for the next batch. */
    Long getWalletId();

    UUID getWalletUuid();

    UUID getUserUuid();

    String getCurrencyCode();

    BigDecimal getWalletBalance();

    /** Zero when the wallet has no ledger account yet, which is itself a divergence worth seeing. */
    BigDecimal getLedgerBalance();
}
