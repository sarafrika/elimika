package apps.sarafrika.elimika.wallet.mapper;

import apps.sarafrika.elimika.wallet.dto.WalletDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletDTO toDto(UserWallet wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletDTO(
                wallet.getUuid(),
                wallet.getUserUuid(),
                wallet.getCurrencyCode(),
                wallet.getBalanceAmount(),
                wallet.getCreatedDate(),
                wallet.getLastModifiedDate()
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
