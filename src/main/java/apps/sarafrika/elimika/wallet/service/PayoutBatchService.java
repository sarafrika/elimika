package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin-approved payout batches. One admin gathers withdrawals waiting for review into a draft and
 * submits it; a different admin approves it once the payout float covers it. Only then are its
 * rows handed to M-Pesa, a few at a time, by {@link PayoutBatchSender}.
 */
public interface PayoutBatchService {

    Page<PayoutBatch> list(Collection<PayoutBatchStatus> statuses, Pageable pageable);

    PayoutBatch get(UUID batchUuid);

    List<Withdrawal> rows(UUID batchUuid);

    /** Paid, failed and in-flight rows of each batch; batches without rows are absent. */
    Map<UUID, RowCounts> counts(Collection<UUID> batchUuids);

    PayoutBatch create(Collection<UUID> withdrawalUuids);

    PayoutBatch addWithdrawals(UUID batchUuid, Collection<UUID> withdrawalUuids);

    PayoutBatch removeWithdrawal(UUID batchUuid, UUID withdrawalUuid);

    PayoutBatch submit(UUID batchUuid);

    /**
     * Releases the batch to M-Pesa. Refused for whoever built or submitted it, when the payout float
     * is below the batch total, and when the float is unknown unless {@code overrideUnknownFloat}.
     */
    default PayoutBatch approve(UUID batchUuid, boolean overrideUnknownFloat) {
        return approve(batchUuid, overrideUnknownFloat, false);
    }

    /**
     * As {@link #approve(UUID, boolean)}, and also refused while any row's wallet check raises a wallet
     * flag unless {@code acknowledgeWalletIssues}, which the approval audit records.
     */
    PayoutBatch approve(UUID batchUuid, boolean overrideUnknownFloat, boolean acknowledgeWalletIssues);

    /** Each batch's rows checked against their wallets now; batches without rows are absent. */
    Map<UUID, WalletIssues> walletIssues(Collection<UUID> batchUuids);

    PayoutBatch reject(UUID batchUuid, String reason);

    PayoutBatch returnToDraft(UUID batchUuid);

    PayoutBatch cancel(UUID batchUuid);

    /** Puts one withdrawal in a new batch, submitted by the caller, for a different admin to approve. */
    Withdrawal submitSingle(UUID withdrawalUuid);

    /** Closes a released batch once every row has finished; does nothing before that. */
    void settle(UUID batchUuid);

    record RowCounts(long paid, long failed, long inFlight) {

        public static final RowCounts NONE = new RowCounts(0, 0, 0);
    }

    /** Rows checked, and how many of them carry a wallet flag. */
    record WalletIssues(int checked, int issues) {

        public static final WalletIssues NONE = new WalletIssues(0, 0);

        public static WalletIssues of(Collection<WalletCheck> checks) {
            return new WalletIssues(checks.size(), (int) checks.stream().filter(WalletCheck::hasIssues).count());
        }
    }
}
