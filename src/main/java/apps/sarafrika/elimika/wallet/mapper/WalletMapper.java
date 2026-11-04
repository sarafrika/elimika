package apps.sarafrika.elimika.wallet.mapper;

import apps.sarafrika.elimika.wallet.dto.WalletDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurseBalance;
import apps.sarafrika.elimika.wallet.service.WalletPurseSummary;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class WalletMapper {

    private WalletMapper() {
    }

    /** One purse row, as a deposit, sale credit or transfer returns it. */
    public static WalletDTO toDto(UserWallet wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletDTO(
                wallet.getUuid(),
                wallet.getOwnerType() == WalletOwnerType.USER ? wallet.getOwnerUuid() : null,
                wallet.getOwnerType(),
                wallet.getOwnerUuid(),
                wallet.getCurrencyCode(),
                wallet.getBalanceAmount(),
                wallet.getCreatedDate(),
                wallet.getLastModifiedDate(),
                null, null, null, null, null, null, null,
                wallet.getPurse(),
                null,
                null
        );
    }

    /**
     * The owner's wallet: every purse listed, and the top-level figures summed over them. Moves between the owner's
     * own purses are left out of the summed transferred_out_total, which is what {@code totals} already holds.
     */
    public static WalletDTO toDto(WalletOwner owner, String currencyCode, List<WalletPurseBalance> purses,
                                  WalletTotals totals, WalletStanding standing) {
        BigDecimal balance = purses.stream().map(WalletPurseBalance::balanceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal held = purses.stream().map(WalletPurseBalance::heldAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        UUID oldest = purses.stream().filter(p -> p.walletUuid() != null && p.createdDate() != null)
                .min(Comparator.comparing(WalletPurseBalance::createdDate))
                .map(WalletPurseBalance::walletUuid).orElse(null);
        LocalDateTime created = purses.stream().map(WalletPurseBalance::createdDate).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime updated = purses.stream().map(WalletPurseBalance::updatedDate).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new WalletDTO(
                oldest,
                owner.type() == WalletOwnerType.USER ? owner.uuid() : null,
                owner.type(),
                owner.uuid(),
                currencyCode,
                balance,
                created,
                updated,
                totals.sharesTotal(),
                totals.platformFeesTotal(),
                totals.salesCount(),
                totals.transferredOutTotal(),
                held,
                balance,
                standing == null ? null : standing.canWithdraw(),
                null,
                balance.add(held),
                purses.stream().map(WalletPurseSummary::from).toList()
        );
    }

    public static WalletTransactionDTO toDto(UserWalletTransaction transaction) {
        return toDto(transaction, null);
    }

    /** With the sale split the row belongs to, for SALE and PLATFORM_FEE rows. */
    public static WalletTransactionDTO toDto(UserWalletTransaction transaction, UUID saleSplitUuid) {
        if (transaction == null) {
            return null;
        }
        UserWallet wallet = transaction.getWallet();
        return new WalletTransactionDTO(
                transaction.getUuid(),
                wallet == null ? null : wallet.getUuid(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrencyCode(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getTransferReference(),
                transaction.getCounterpartyUserUuid(),
                transaction.getCreatedDate(),
                saleSplitUuid,
                wallet == null ? null : wallet.getPurse()
        );
    }
}
