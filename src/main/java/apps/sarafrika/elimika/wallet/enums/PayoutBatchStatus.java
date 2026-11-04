package apps.sarafrika.elimika.wallet.enums;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** A payout batch's life. Listed in chk_payout_batch_status. */
public enum PayoutBatchStatus {
    /** Being built; rows can be added and removed. */
    DRAFT,
    /** Submitted; waiting for a different admin to approve or reject it. */
    AWAITING_APPROVAL,
    /** Approved; its rows are being handed to M-Pesa. */
    RELEASED,
    /** Every row was paid. */
    COMPLETED,
    /** Every row finished, and at least one was not paid. */
    PARTLY_FAILED,
    REJECTED,
    CANCELLED;

    /** A withdrawal in a batch with one of these statuses cannot join another. */
    public static final Set<PayoutBatchStatus> OPEN = EnumSet.of(DRAFT, AWAITING_APPROVAL, RELEASED);

    public static PayoutBatchStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PayoutBatchStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
