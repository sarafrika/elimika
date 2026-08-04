package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A cached running balance, in the account's natural sign (debit-normal accounts count debits as
 * positive, credit-normal accounts count credits as positive).
 * <p>
 * Derived and rebuildable: {@code ledger_entries} is the truth, and this row can always be
 * recomputed from it. It exists so a balance read is not an aggregate over the whole history.
 */
@Entity
@Table(name = "ledger_account_balances")
@Getter
@Setter
@NoArgsConstructor
public class LedgerAccountBalance extends BaseEntity {

    @Column(name = "account_uuid")
    private UUID accountUuid;

    @Column(name = "posted_amount")
    private BigDecimal postedAmount = BigDecimal.ZERO;

    @Column(name = "pending_amount")
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Version
    @Column(name = "version")
    private Long version;
}
