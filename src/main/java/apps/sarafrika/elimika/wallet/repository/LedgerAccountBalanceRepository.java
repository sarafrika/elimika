package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.LedgerAccountBalance;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerAccountBalanceRepository extends JpaRepository<LedgerAccountBalance, Long> {

    Optional<LedgerAccountBalance> findByAccountUuid(UUID accountUuid);

    /**
     * Applies a signed delta to the cached balance.
     * <p>
     * Deliberately an atomic upsert rather than a JPA read-modify-write: two concurrent postings to
     * the same account would otherwise either lose an update or collide on the optimistic version
     * and need a retry loop. {@code version} is still bumped so a stale JPA copy of the row cannot
     * be written back over this.
     */
    @Modifying
    @Query(value = """
            insert into ledger_account_balances (account_uuid, posted_amount, pending_amount, version, created_by)
            values (:accountUuid, :delta, 0, 0, 'SYSTEM')
            on conflict (account_uuid) do update
                set posted_amount = ledger_account_balances.posted_amount + excluded.posted_amount,
                    version       = ledger_account_balances.version + 1,
                    updated_date  = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void applyPostedDelta(@Param("accountUuid") UUID accountUuid, @Param("delta") BigDecimal delta);
}
