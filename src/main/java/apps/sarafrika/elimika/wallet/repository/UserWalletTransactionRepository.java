package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /** An owner's rows in the given purses and types recorded in {@code [from, to)}, newest first. */
    @EntityGraph(attributePaths = "wallet")
    @Query("select txn from UserWalletTransaction txn where txn.wallet.ownerType = :ownerType "
            + "and txn.wallet.ownerUuid = :ownerUuid and txn.wallet.currencyCode = :currency "
            + "and txn.wallet.purse in :purses "
            + "and txn.transactionType in :types and txn.createdDate >= :from and txn.createdDate < :to "
            + "order by txn.createdDate desc, txn.id desc")
    Page<UserWalletTransaction> findFiltered(@Param("ownerType") WalletOwnerType ownerType,
                                             @Param("ownerUuid") UUID ownerUuid,
                                             @Param("currency") String currency,
                                             @Param("purses") Collection<WalletPurse> purses,
                                             @Param("types") Collection<WalletTransactionType> types,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             Pageable pageable);

    /** An owner's rows of the given types and references, in every purse, oldest first. */
    @EntityGraph(attributePaths = "wallet")
    @Query("select txn from UserWalletTransaction txn where txn.wallet.ownerType = :ownerType "
            + "and txn.wallet.ownerUuid = :ownerUuid and txn.wallet.currencyCode = :currency "
            + "and txn.transactionType in :types and txn.reference in :references order by txn.id asc")
    List<UserWalletTransaction> findOwnerRows(@Param("ownerType") WalletOwnerType ownerType,
                                              @Param("ownerUuid") UUID ownerUuid,
                                              @Param("currency") String currency,
                                              @Param("types") Collection<WalletTransactionType> types,
                                              @Param("references") Collection<String> references);

    @EntityGraph(attributePaths = "wallet")
    Optional<UserWalletTransaction> findFirstByReferenceAndTransactionTypeOrderByIdAsc(String reference,
                                                                                    WalletTransactionType type);

    /**
     * One row per calendar month with activity, newest first. In and out are the balance moves, so
     * every transaction type counts by what it did to the balance. With {@code excludeOwnMoves}, money moved
     * between the owner's own purses is left out of in, out and the count (the balances still include it), so
     * purse statements summed into one do not show it twice.
     */
    @Query(value = """
            select to_char(date_trunc('month', t.created_date at time zone 'UTC'), 'YYYY-MM') as month,
                   coalesce(sum(greatest(t.balance_after - t.balance_before, 0))
                            filter (where not (:excludeOwnMoves and coalesce(t.counterparty_user_uuid = w.user_uuid, false))), 0) as moneyIn,
                   coalesce(sum(greatest(t.balance_before - t.balance_after, 0))
                            filter (where not (:excludeOwnMoves and coalesce(t.counterparty_user_uuid = w.user_uuid, false))), 0) as moneyOut,
                   count(*) filter (where not (:excludeOwnMoves and coalesce(t.counterparty_user_uuid = w.user_uuid, false)))
                       as transactionCount,
                   (array_agg(t.balance_before order by t.created_date, t.id))[1] as openingBalance,
                   (array_agg(t.balance_after order by t.created_date desc, t.id desc))[1] as closingBalance
            from user_wallet_transactions t
                     join user_wallets w on w.id = t.wallet_id
            where t.wallet_id = :walletId
            group by 1
            order by 1 desc
            """, nativeQuery = true)
    List<MonthlyWalletActivity> monthlyActivity(@Param("walletId") Long walletId,
                                                @Param("excludeOwnMoves") boolean excludeOwnMoves);

    boolean existsByReference(String reference);

    /** Sum and count per transaction type for one wallet row; each row is {type, sum, count}. */
    @Query("select txn.transactionType, coalesce(sum(txn.amount), 0), count(txn) "
            + "from UserWalletTransaction txn where txn.wallet.id = :walletId group by txn.transactionType")
    List<Object[]> totalsByType(@Param("walletId") Long walletId);

    /** What a wallet row sent to the owner's other purses: transfers out whose counterparty is the owner. */
    @Query("select coalesce(sum(txn.amount), 0) from UserWalletTransaction txn where txn.wallet.id = :walletId "
            + "and txn.transactionType = :type and txn.counterpartyUserUuid = txn.wallet.ownerUuid")
    java.math.BigDecimal sumOwnMoves(@Param("walletId") Long walletId, @Param("type") WalletTransactionType type);

    @EntityGraph(attributePaths = "wallet")
    @Query("select txn from UserWalletTransaction txn where txn.reference in :references order by txn.id asc")
    List<UserWalletTransaction> findByReferenceInWithWallet(@Param("references") Collection<String> references);
}
