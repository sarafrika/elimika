package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

public enum WalletTransactionType {
    DEPOSIT,
    SALE,
    TRANSFER_IN,
    TRANSFER_OUT,
    /** The platform's fee on a sale share, debited right after the share itself. */
    PLATFORM_FEE;

    public static WalletTransactionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletTransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
