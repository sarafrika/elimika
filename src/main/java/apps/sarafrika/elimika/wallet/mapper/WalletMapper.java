package apps.sarafrika.elimika.wallet.mapper;

import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import apps.sarafrika.elimika.wallet.dto.WalletDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletDTO toDto(UserWallet wallet) {
        return toDto(wallet, null, null);
    }

    /** With lifetime totals and the caller's standing, for the screens that show them. */
    public static WalletDTO toDto(UserWallet wallet, WalletTotals totals, WalletStanding standing) {
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
                totals == null ? null : totals.sharesTotal(),
                totals == null ? null : totals.platformFeesTotal(),
                totals == null ? null : totals.salesCount(),
                totals == null ? null : totals.transferredOutTotal(),
                standing == null ? null : standing.heldAmount(),
                standing == null ? null : standing.availableAmount(),
                standing == null ? null : standing.canWithdraw()
        );
    }

    public static WalletTransactionDTO toDto(UserWalletTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new WalletTransactionDTO(
                transaction.getUuid(),
                transaction.getWallet() == null ? null : transaction.getWallet().getUuid(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrencyCode(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getTransferReference(),
                transaction.getCounterpartyUserUuid(),
                transaction.getCreatedDate()
        );
    }
}
