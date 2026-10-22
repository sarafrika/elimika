package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Which rows of a wallet to read: some types, between two UTC dates inclusive; empty or null means all. */
public record WalletTransactionFilter(Set<WalletTransactionType> types, LocalDate from, LocalDate to) {

    private static final LocalDateTime EARLIEST = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime LATEST = LocalDateTime.of(9999, 1, 1, 0, 0);

    public static WalletTransactionFilter all() {
        return new WalletTransactionFilter(null, null, null);
    }

    public static WalletTransactionFilter of(Collection<WalletTransactionType> types, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date");
        }
        return new WalletTransactionFilter(types == null || types.isEmpty() ? null : EnumSet.copyOf(types), from, to);
    }

    public Set<WalletTransactionType> typesOrAll() {
        return types == null ? EnumSet.allOf(WalletTransactionType.class) : types;
    }

    public LocalDateTime fromInclusive() {
        return from == null ? EARLIEST : from.atStartOfDay();
    }

    public LocalDateTime toExclusive() {
        return to == null ? LATEST : to.plusDays(1).atStartOfDay();
    }
}
