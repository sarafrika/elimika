package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/** Everything a wallet's audit trail records. Each value is also listed in chk_wallet_audit_event_type. */
public enum WalletAuditEventType {
    DEPOSIT_RECORDED,
    SALE_SHARE_CREDITED,
    PLATFORM_FEE_CHARGED,
    TRANSFER_SENT,
    TRANSFER_RECEIVED,
    SALE_SHARE_REVERSED;

    public static WalletAuditEventType forTransaction(WalletTransactionType type) {
        return switch (type) {
            case DEPOSIT -> DEPOSIT_RECORDED;
            case SALE -> SALE_SHARE_CREDITED;
            case PLATFORM_FEE -> PLATFORM_FEE_CHARGED;
            case TRANSFER_OUT -> TRANSFER_SENT;
            case TRANSFER_IN -> TRANSFER_RECEIVED;
            case SALE_REVERSAL -> SALE_SHARE_REVERSED;
        };
    }

    public static WalletAuditEventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletAuditEventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
