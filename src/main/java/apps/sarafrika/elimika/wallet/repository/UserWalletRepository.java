package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUuid(UUID uuid);

    Optional<UserWallet> findByUserUuidAndCurrencyCode(UUID userUuid, String currencyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from UserWallet wallet where wallet.userUuid = :userUuid and wallet.currencyCode = :currencyCode")
    Optional<UserWallet> findLockedByUserUuidAndCurrencyCode(
            @Param("userUuid") UUID userUuid,
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
     * A user's earnings account is credit-normal (the platform owes the money), so its natural
     * balance is credits minus debits.
     */
    @Query(value = """
            select w.id             as "walletId",
                   w.uuid           as "walletUuid",
                   w.user_uuid      as "userUuid",
                   w.currency_code  as "currencyCode",
                   w.balance_amount as "walletBalance",
                   coalesce((select sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end)
                             from ledger_entries e
                             where e.account_uuid = la.uuid), 0) as "ledgerBalance"
            from user_wallets w
                     left join ledger_accounts la
                               on la.owner_type = 'USER'
                                   and la.owner_uuid = w.user_uuid
                                   and la.purse = 'EARNINGS'
                                   and la.currency_code = w.currency_code
            where w.id > :cursor
            order by w.id
            limit :batchSize
            """, nativeQuery = true)
    List<WalletLedgerComparison> compareWalletsAgainstLedger(
            @Param("cursor") long cursor,
            @Param("batchSize") int batchSize
    );
}
