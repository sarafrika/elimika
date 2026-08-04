package apps.sarafrika.elimika.wallet.ledger;

import apps.sarafrika.elimika.wallet.enums.LedgerAccountType;
import apps.sarafrika.elimika.wallet.enums.LedgerOwnerType;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import java.util.Objects;
import java.util.UUID;

/**
 * The identity of an account, as callers name it. Resolved (and created on first use) by
 * {@link LedgerService#getOrCreateAccountUuid(LedgerAccountRef)}.
 */
public record LedgerAccountRef(
        LedgerOwnerType ownerType,
        UUID ownerUuid,
        LedgerAccountType accountType,
        LedgerPurse purse,
        String currencyCode
) {

    public LedgerAccountRef {
        Objects.requireNonNull(ownerType, "ownerType is required");
        Objects.requireNonNull(accountType, "accountType is required");
        Objects.requireNonNull(purse, "purse is required");
        Objects.requireNonNull(currencyCode, "currencyCode is required");
        if (ownerType != LedgerOwnerType.PLATFORM && ownerUuid == null) {
            throw new IllegalArgumentException("Only platform accounts may be ownerless");
        }
        if (ownerType == LedgerOwnerType.PLATFORM && ownerUuid != null) {
            throw new IllegalArgumentException("Platform accounts do not have an owner");
        }
    }

    /** A party's spendable balance. Money the platform holds and owes, hence a liability. */
    public static LedgerAccountRef userEarnings(UUID userUuid, String currencyCode) {
        return new LedgerAccountRef(
                LedgerOwnerType.USER, userUuid, LedgerAccountType.LIABILITY, LedgerPurse.EARNINGS, currencyCode);
    }

    public static LedgerAccountRef platform(LedgerAccountType accountType, LedgerPurse purse, String currencyCode) {
        return new LedgerAccountRef(LedgerOwnerType.PLATFORM, null, accountType, purse, currencyCode);
    }

    /** Where money received but not yet attributed to anybody sits. */
    public static LedgerAccountRef platformUnallocatedRevenue(String currencyCode) {
        return platform(LedgerAccountType.REVENUE, LedgerPurse.PLATFORM_UNALLOCATED_REVENUE, currencyCode);
    }

    /** Cash the platform actually holds at the M-Pesa rail. */
    public static LedgerAccountRef platformCashMpesa(String currencyCode) {
        return platform(LedgerAccountType.ASSET, LedgerPurse.PLATFORM_CASH_MPESA, currencyCode);
    }
}
