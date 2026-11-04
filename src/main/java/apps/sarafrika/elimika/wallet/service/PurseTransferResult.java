package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import java.util.UUID;

/** Money moved between two purses of one owner; both rows share {@code transferReference}. */
public record PurseTransferResult(UserWallet fromWallet, UserWallet toWallet, UUID transferReference) {
}
