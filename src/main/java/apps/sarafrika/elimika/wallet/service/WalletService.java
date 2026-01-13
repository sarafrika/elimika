package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    UserWallet getOrCreateWallet(UUID userUuid, String currencyCode);

    Page<UserWalletTransaction> getTransactions(UUID userUuid, String currencyCode, Pageable pageable);

    UserWallet deposit(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description);

    UserWallet creditSale(UUID userUuid, BigDecimal amount, String currencyCode, String reference, String description);

    WalletTransferResult transfer(UUID fromUserUuid, UUID toUserUuid, BigDecimal amount, String currencyCode, String reference, String description);
}
