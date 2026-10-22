package apps.sarafrika.elimika.wallet.enums;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** A withdrawal's life. Listed in chk_withdrawal_status. */
public enum WithdrawalStatus {
    /** Requested; waiting for the SMS code. No money has moved. */
    AWAITING_CONFIRMATION,
    /** Confirmed and held; an organisation's second approver must agree. */
    AWAITING_SECOND_APPROVAL,
    /** Confirmed and held; an admin must approve it. */
    NEEDS_REVIEW,
    /** Cleared to pay; being handed to M-Pesa. */
    SENDING,
    /** With M-Pesa; waiting for its result. */
    PROCESSING,
    PAID,
    FAILED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    /** Confirmed but not finished: the money is out of the balance and held. */
    public static final Set<WithdrawalStatus> HELD = EnumSet.of(AWAITING_SECOND_APPROVAL, NEEDS_REVIEW, SENDING, PROCESSING);

    /** Statuses that count towards the daily limit. */
    public static final Set<WithdrawalStatus> COUNTED = EnumSet.of(AWAITING_CONFIRMATION, AWAITING_SECOND_APPROVAL,
            NEEDS_REVIEW, SENDING, PROCESSING, PAID);

    public static WithdrawalStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WithdrawalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
