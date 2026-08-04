package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionUuid(UUID transactionUuid);

    List<LedgerEntry> findByAccountUuid(UUID accountUuid);

    long countByTransactionUuid(UUID transactionUuid);

    /**
     * The account's balance recomputed from the entries, in the account's natural sign: debits
     * increase an asset or expense account, credits increase a liability or revenue account.
     */
    @Query(value = """
            select coalesce(sum(
                case
                    when a.account_type in ('ASSET', 'EXPENSE')
                        then case when e.direction = 'DEBIT' then e.amount else -e.amount end
                    else case when e.direction = 'CREDIT' then e.amount else -e.amount end
                end), 0)
            from ledger_entries e
            join ledger_accounts a on a.uuid = e.account_uuid
            where e.account_uuid = :accountUuid
            """, nativeQuery = true)
    BigDecimal derivedBalance(@Param("accountUuid") UUID accountUuid);
}
