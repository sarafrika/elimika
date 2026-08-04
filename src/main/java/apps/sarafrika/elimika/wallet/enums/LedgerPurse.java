package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/**
 * The purse identifies an account within a single owner.
 * <p>
 * Party-owned accounts have exactly one purse in this phase - {@link #EARNINGS}. The full purse
 * taxonomy (restricted funds and the rest) is phase 2 and deliberately absent here.
 * <p>
 * The platform's internal accounts share {@code owner_type = PLATFORM} with no owner UUID, so the
 * purse is also what tells them apart. That is why the platform account names live in this enum
 * rather than in a column of their own: the account identity is
 * {@code (owner_type, owner_uuid, purse, currency_code)}, and nothing else in that tuple varies
 * between them.
 */
public enum LedgerPurse {

    /** A party's spendable balance. Mirrors {@code user_wallets.balance_amount}. */
    EARNINGS,

    /** Platform asset: money actually received through M-Pesa. */
    PLATFORM_CASH_MPESA,

    /** Platform revenue: the platform's own take. */
    PLATFORM_FEE_REVENUE,

    /** Platform revenue: received but not yet attributed to an earner or a fee. */
    PLATFORM_UNALLOCATED_REVENUE,

    /** Platform liability: money committed to a payout that has not yet settled. */
    PAYOUTS_IN_FLIGHT,

    /** Platform expense: what the payout rails charge. */
    PAYOUT_FEES;

    public static LedgerPurse fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LedgerPurse.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
