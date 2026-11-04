package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Checks the wallets behind withdrawals an admin is reviewing, a page at a time. */
public interface WithdrawalWalletChecks {

    /** Keyed by withdrawal uuid; two statements however many rows and owners. */
    Map<UUID, WalletCheck> check(Collection<Withdrawal> withdrawals);
}
