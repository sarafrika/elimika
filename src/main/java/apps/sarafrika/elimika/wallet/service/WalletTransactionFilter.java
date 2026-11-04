package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which rows of a wallet to read: one purse, some types, between two UTC dates inclusive; empty or null means all.
 */
public record WalletTransactionFilter(Set<WalletTransactionType> types, LocalDate from, LocalDate to,
                                      WalletPurse purse) {

    private static final LocalDateTime EARLIEST = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime LATEST = LocalDateTime.of(9999, 1, 1, 0, 0);

    public static WalletTransactionFilter all() {
        return new WalletTransactionFilter(null, null, null, null);
    }

    public static WalletTransactionFilter of(Collection<WalletTransactionType> types, LocalDate from, LocalDate to) {
        return of(types, from, to, null);
    }

    public static WalletTransactionFilter of(Collection<WalletTransactionType> types, LocalDate from, LocalDate to,
                                             WalletPurse purse) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date");
        }
        return new WalletTransactionFilter(types == null || types.isEmpty() ? null : EnumSet.copyOf(types), from, to,
                purse);
    }

    public Set<WalletTransactionType> typesOrAll() {
        return types == null ? EnumSet.allOf(WalletTransactionType.class) : types;
    }

    /** The purse asked for, or every purse the owner type can hold. */
    public List<WalletPurse> pursesFor(WalletOwnerType ownerType) {
        if (purse == null) {
            return WalletPurse.of(ownerType);
        }
        if (!purse.isAllowedFor(ownerType)) {
            throw new IllegalArgumentException("A " + ownerType.name().toLowerCase(Locale.ROOT)
                    + " wallet has no " + purse + " purse");
        }
        return List.of(purse);
    }

    public LocalDateTime fromInclusive() {
        return from == null ? EARLIEST : from.atStartOfDay();
    }

    public LocalDateTime toExclusive() {
        return to == null ? LATEST : to.plusDays(1).atStartOfDay();
    }
}
