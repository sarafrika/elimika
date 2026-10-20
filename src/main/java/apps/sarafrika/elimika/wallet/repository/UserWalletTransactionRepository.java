package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWalletTransactionRepository extends JpaRepository<UserWalletTransaction, Long> {

    // WalletController maps the page after the transaction ends, reading txn.wallet.uuid; fetch it here.
    @EntityGraph(attributePaths = "wallet")
    @Query("select txn from UserWalletTransaction txn where txn.wallet.uuid = :walletUuid order by txn.id desc")
    Page<UserWalletTransaction> findByWalletUuid(@Param("walletUuid") UUID walletUuid, Pageable pageable);

    boolean existsByReference(String reference);

    /** Sum and count per transaction type for one wallet; each row is {type, sum, count}. */
    @Query("select txn.transactionType, coalesce(sum(txn.amount), 0), count(txn) "
            + "from UserWalletTransaction txn where txn.wallet.id = :walletId group by txn.transactionType")
    List<Object[]> totalsByType(@Param("walletId") Long walletId);
}
