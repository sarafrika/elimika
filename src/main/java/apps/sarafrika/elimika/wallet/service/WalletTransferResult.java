package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import java.util.UUID;

public record WalletTransferResult(
        UserWallet sourceWallet,
        UserWallet targetWallet,
        UUID transferReference
) {
}
