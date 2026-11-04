package apps.sarafrika.elimika.wallet.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which of an owner's balances a wallet row holds. A person keeps one purse per role, each with its own balance;
 * an organisation has its one {@link #EARNINGS} purse. The ledger names its liability accounts the same way.
 */
public enum WalletPurse {

    /** A person's creator legs of class sales, net of the platform fee on them. */
    COURSE_CREATOR,

    /** A person's delivery legs of class sales they taught, net of the platform fee on them. */
    INSTRUCTOR,

    /**
     * A person's learner-side credit: deposits, transfers received and moves from their other purses. Spend-only:
     * it is never withdrawn and never moved to another purse, or it would become withdrawable.
     */
    STUDENT,

    /** An organisation's earnings as the delivery party of the classes it hires for. */
    EARNINGS;

    private static final Set<WalletPurse> USER_PURSES = EnumSet.of(COURSE_CREATOR, INSTRUCTOR, STUDENT);

    /** The purses an owner of this type can hold, in display order. */
    public static List<WalletPurse> of(WalletOwnerType ownerType) {
        return ownerType == WalletOwnerType.ORGANISATION ? List.of(EARNINGS) : List.copyOf(USER_PURSES);
    }

    public static final String STUDENT_NOT_WITHDRAWABLE = "The STUDENT purse can only be spent on Elimika; "
            + "it cannot be withdrawn. Choose your COURSE_CREATOR or INSTRUCTOR purse.";
    public static final String STUDENT_NOT_MOVABLE = "The STUDENT purse can only be spent on Elimika; "
            + "money in it cannot be moved to another purse.";

    /** Whether money in this purse may be paid out or moved to another purse. */
    public boolean isWithdrawable() {
        return this != STUDENT;
    }

    public boolean isAllowedFor(WalletOwnerType ownerType) {
        return ownerType == WalletOwnerType.ORGANISATION ? this == EARNINGS : USER_PURSES.contains(this);
    }

    /** Where a creator leg of a sale is paid: a person's COURSE_CREATOR purse, an organisation's EARNINGS. */
    public static WalletPurse creatorLegOf(WalletOwnerType ownerType) {
        return ownerType == WalletOwnerType.ORGANISATION ? EARNINGS : COURSE_CREATOR;
    }

    /** Where a delivery leg of a sale is paid: a person's INSTRUCTOR purse, an organisation's EARNINGS. */
    public static WalletPurse deliveryLegOf(WalletOwnerType ownerType) {
        return ownerType == WalletOwnerType.ORGANISATION ? EARNINGS : INSTRUCTOR;
    }

    public static WalletPurse fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletPurse.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
