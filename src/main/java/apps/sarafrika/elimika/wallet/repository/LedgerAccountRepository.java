package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.LedgerAccount;
import apps.sarafrika.elimika.wallet.enums.LedgerOwnerType;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    Optional<LedgerAccount> findByUuid(UUID uuid);

    /**
     * Identity lookup. {@code ownerUuid} is null for platform accounts, so the comparison has to be
     * null-safe rather than a plain equality - the same reason the unique index coalesces it.
     */
    @Query("""
            select account from LedgerAccount account
            where account.ownerType = :ownerType
              and ((:ownerUuid is null and account.ownerUuid is null) or account.ownerUuid = :ownerUuid)
              and account.purse = :purse
              and account.currencyCode = :currencyCode
            """)
    Optional<LedgerAccount> findByIdentity(
            @Param("ownerType") LedgerOwnerType ownerType,
            @Param("ownerUuid") UUID ownerUuid,
            @Param("purse") LedgerPurse purse,
            @Param("currencyCode") String currencyCode
    );

    /**
     * Provisions an account without ever raising on a concurrent creator.
     * <p>
     * A losing race on {@code uq_ledger_account_identity} would otherwise throw, and a constraint
     * violation marks the whole transaction rollback-only - so a second caller creating the same
     * account would destroy an unrelated posting. {@code ON CONFLICT DO NOTHING} makes that a
     * no-op instead, and the caller simply re-reads the winner's row.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into ledger_accounts (owner_type, owner_uuid, account_type, purse, currency_code, status, created_by)
            values (:ownerType, cast(:ownerUuid as uuid), :accountType, :purse, :currencyCode, 'ACTIVE', 'SYSTEM')
            on conflict do nothing
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("ownerType") String ownerType,
            @Param("ownerUuid") UUID ownerUuid,
            @Param("accountType") String accountType,
            @Param("purse") String purse,
            @Param("currencyCode") String currencyCode
    );
}
