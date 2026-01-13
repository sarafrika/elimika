package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWalletTransactionRepository extends JpaRepository<UserWalletTransaction, Long> {

    @Query("select txn from UserWalletTransaction txn where txn.wallet.uuid = :walletUuid order by txn.id desc")
    Page<UserWalletTransaction> findByWalletUuid(@Param("walletUuid") UUID walletUuid, Pageable pageable);
}
