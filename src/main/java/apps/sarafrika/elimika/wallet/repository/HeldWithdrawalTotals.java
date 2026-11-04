package apps.sarafrika.elimika.wallet.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** What an owner has held in confirmed, unfinished withdrawals, and what the wallet actually debited for them. */
public interface HeldWithdrawalTotals {

    String getOwnerType();

    UUID getOwnerUuid();

    String getCurrencyCode();

    long getHeldCount();

    BigDecimal getHeldAmount();

    /** Withdrawal and fee debits recorded against the wallet for those withdrawals, less any reversal. */
    BigDecimal getHeldDebited();
}
