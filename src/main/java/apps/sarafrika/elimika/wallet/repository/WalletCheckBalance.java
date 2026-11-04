package apps.sarafrika.elimika.wallet.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** A wallet's balance beside its ledger earnings balance, for the admin wallet check. */
public interface WalletCheckBalance {

    String getOwnerType();

    UUID getOwnerUuid();

    String getCurrencyCode();

    BigDecimal getWalletBalance();

    /** The cached posted balance of the wallet's EARNINGS account; null when it has no ledger account. */
    BigDecimal getLedgerBalance();
}
