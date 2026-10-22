package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import java.time.LocalDateTime;
import java.util.Collection;
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

    /** One wallet's rows of the given types recorded in {@code [from, to)}, newest first. */
    @EntityGraph(attributePaths = "wallet")
    @Query("select txn from UserWalletTransaction txn where txn.wallet.uuid = :walletUuid "
            + "and txn.transactionType in :types and txn.createdDate >= :from and txn.createdDate < :to "
            + "order by txn.id desc")
    Page<UserWalletTransaction> findFiltered(@Param("walletUuid") UUID walletUuid,
                                             @Param("types") Collection<WalletTransactionType> types,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             Pageable pageable);

    /**
     * One row per calendar month with activity, newest first. In and out are the balance moves, so
     * every transaction type counts by what it did to the balance.
     */
    @Query(value = """
            select to_char(date_trunc('month', t.created_date at time zone 'UTC'), 'YYYY-MM') as month,
                   coalesce(sum(greatest(t.balance_after - t.balance_before, 0)), 0) as moneyIn,
                   coalesce(sum(greatest(t.balance_before - t.balance_after, 0)), 0) as moneyOut,
                   count(*) as transactionCount,
                   (array_agg(t.balance_before order by t.created_date, t.id))[1] as openingBalance,
                   (array_agg(t.balance_after order by t.created_date desc, t.id desc))[1] as closingBalance
            from user_wallet_transactions t
            where t.wallet_id = :walletId
            group by 1
            order by 1 desc
            """, nativeQuery = true)
    List<MonthlyWalletActivity> monthlyActivity(@Param("walletId") Long walletId);

    boolean existsByReference(String reference);

    /** Sum and count per transaction type for one wallet; each row is {type, sum, count}. */
    @Query("select txn.transactionType, coalesce(sum(txn.amount), 0), count(txn) "
            + "from UserWalletTransaction txn where txn.wallet.id = :walletId group by txn.transactionType")
    List<Object[]> totalsByType(@Param("walletId") Long walletId);
}
