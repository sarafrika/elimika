package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/** Everything a wallet's audit trail records. Each value is also listed in chk_wallet_audit_event_type. */
public enum WalletAuditEventType {
    DEPOSIT_RECORDED,
    SALE_SHARE_CREDITED,
    PLATFORM_FEE_CHARGED,
    TRANSFER_SENT,
    TRANSFER_RECEIVED,
    SALE_SHARE_REVERSED,
    PAYOUT_METHOD_ADDED,
    PAYOUT_METHOD_VERIFIED,
    PAYOUT_METHOD_REMOVED,
    PAYOUT_METHOD_DEFAULT_CHANGED,
    PAYOUT_APPROVER_ADDED,
    PAYOUT_APPROVER_REMOVED,
    WITHDRAWAL_REQUESTED,
    WITHDRAWAL_CONFIRMED,
    /** The money leaving the balance for a confirmed withdrawal. */
    WITHDRAWAL_HELD,
    WITHDRAWAL_SECOND_APPROVED,
    WITHDRAWAL_APPROVED,
    WITHDRAWAL_REJECTED,
    WITHDRAWAL_PAID,
    WITHDRAWAL_FAILED,
    WITHDRAWAL_REVERSED,
    WITHDRAWAL_CANCELLED,
    PAYOUT_FEE_CHARGED,
    PAYOUT_SETTINGS_CHANGED;

    public static WalletAuditEventType forTransaction(WalletTransactionType type) {
        return switch (type) {
            case DEPOSIT -> DEPOSIT_RECORDED;
            case SALE -> SALE_SHARE_CREDITED;
            case PLATFORM_FEE -> PLATFORM_FEE_CHARGED;
            case TRANSFER_OUT -> TRANSFER_SENT;
            case TRANSFER_IN -> TRANSFER_RECEIVED;
            case SALE_REVERSAL -> SALE_SHARE_REVERSED;
            case WITHDRAWAL -> WITHDRAWAL_HELD;
            case WITHDRAWAL_REVERSAL -> WITHDRAWAL_REVERSED;
            case PAYOUT_FEE -> PAYOUT_FEE_CHARGED;
        };
    }

    public static WalletAuditEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletAuditEventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
