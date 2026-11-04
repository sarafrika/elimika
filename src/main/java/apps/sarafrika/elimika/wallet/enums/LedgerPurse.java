package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/**
 * The purse identifies an account within a single owner.
 * <p>
 * A person holds one liability account per role purse ({@link #COURSE_CREATOR}, {@link #INSTRUCTOR},
 * {@link #STUDENT}), each mirroring that purse's wallet row; an organisation holds {@link #EARNINGS}. A person's
 * EARNINGS account predates the purses and is left at zero by the split.
 * <p>
 * The platform's internal accounts share {@code owner_type = PLATFORM} with no owner UUID, so the
 * purse is also what tells them apart. That is why the platform account names live in this enum
 * rather than in a column of their own: the account identity is
 * {@code (owner_type, owner_uuid, purse, currency_code)}, and nothing else in that tuple varies
 * between them.
 */
public enum LedgerPurse {

    /** An organisation's spendable balance. Mirrors its {@code user_wallets.balance_amount}. */
    EARNINGS,

    /** A person's course creator purse. */
    COURSE_CREATOR,

    /** A person's instructor purse. */
    INSTRUCTOR,

    /** A person's learner purse. */
    STUDENT,

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
