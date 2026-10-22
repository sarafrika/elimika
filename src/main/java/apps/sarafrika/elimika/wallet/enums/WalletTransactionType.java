package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

public enum WalletTransactionType {
    DEPOSIT,
    SALE,
    TRANSFER_IN,
    TRANSFER_OUT,
    /** The platform's fee on a sale share, debited right after the share itself. */
    PLATFORM_FEE,
    /** Takes back a sale credited to the wrong party; written by one-off correction scripts. */
    SALE_REVERSAL,
    /** Money sent to a payout number, taken from the balance when the withdrawal is confirmed. */
    WITHDRAWAL,
    /** A withdrawal's money back in the wallet after M-Pesa failed it or an admin rejected it. */
    WITHDRAWAL_REVERSAL,
    /** The M-Pesa charge on a withdrawal, when the earner pays it. */
    PAYOUT_FEE;

    public static WalletTransactionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletTransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
