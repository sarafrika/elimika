package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUuid(UUID uuid);

    Optional<UserWallet> findByOwnerTypeAndOwnerUuidAndPurseAndCurrencyCode(
            WalletOwnerType ownerType, UUID ownerUuid, WalletPurse purse, String currencyCode);

    /** Every purse row the owner holds in a currency, oldest first. */
    List<UserWallet> findByOwnerTypeAndOwnerUuidAndCurrencyCodeOrderByIdAsc(
            WalletOwnerType ownerType, UUID ownerUuid, String currencyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from UserWallet wallet where wallet.ownerType = :ownerType "
            + "and wallet.ownerUuid = :ownerUuid and wallet.purse = :purse and wallet.currencyCode = :currencyCode")
    Optional<UserWallet> findLocked(
            @Param("ownerType") WalletOwnerType ownerType,
            @Param("ownerUuid") UUID ownerUuid,
            @Param("purse") WalletPurse purse,
            @Param("currencyCode") String currencyCode
    );

    /**
     * One page of wallets alongside the balance derived from their ledger entries, for
     * reconciliation.
     * <p>
     * Keyset paging on {@code id} rather than OFFSET so a large wallet table is walked in bounded,
     * constant-cost slices instead of degrading as the offset grows. The derived balance is
     * computed in the same statement, so a batch of N wallets costs one query rather than N.
     * <p>
     * Each wallet row is one purse and is compared with the ledger account of that purse. A purse
     * account is credit-normal (the platform owes the money), so its natural balance is credits minus
     * debits.
     */
    @Query(value = """
            select w.id             as "walletId",
                   w.uuid           as "walletUuid",
                   w.user_uuid      as "userUuid",
                   w.purse          as "purse",
                   w.currency_code  as "currencyCode",
                   w.balance_amount as "walletBalance",
                   coalesce((select sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end)
                             from ledger_entries e
                             where e.account_uuid = la.uuid), 0) as "ledgerBalance"
            from user_wallets w
                     left join ledger_accounts la
                               on la.owner_type = w.owner_type
                                   and la.owner_uuid = w.user_uuid
                                   and la.purse = w.purse
                                   and la.currency_code = w.currency_code
            where w.id > :cursor
            order by w.id
            limit :batchSize
            """, nativeQuery = true)
    List<WalletLedgerComparison> compareWalletsAgainstLedger(
            @Param("cursor") long cursor,
            @Param("batchSize") int batchSize
    );

    /** The owners' purse rows with the ledger balance of each purse, in one statement. */
    @Query(value = """
            select w.owner_type     as "ownerType",
                   w.user_uuid      as "ownerUuid",
                   w.purse          as "purse",
                   w.currency_code  as "currencyCode",
                   w.balance_amount as "walletBalance",
                   case when la.uuid is null then null else coalesce(lb.posted_amount, 0) end as "ledgerBalance"
            from user_wallets w
                     left join ledger_accounts la
                               on la.owner_type = w.owner_type
                                   and la.owner_uuid = w.user_uuid
                                   and la.purse = w.purse
                                   and la.currency_code = w.currency_code
                     left join ledger_account_balances lb on lb.account_uuid = la.uuid
            where w.user_uuid in (:ownerUuids)
            """, nativeQuery = true)
    List<WalletCheckBalance> findWalletCheckBalances(@Param("ownerUuids") Collection<UUID> ownerUuids);
}
