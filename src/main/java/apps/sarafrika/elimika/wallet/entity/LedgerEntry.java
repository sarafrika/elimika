package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.LedgerEntryDirection;
import apps.sarafrika.elimika.wallet.util.converter.LedgerEntryDirectionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One leg of a transaction. Immutable, like the transaction it belongs to.
 * <p>
 * {@code amount} is always positive; {@link #direction} carries the sign. A deferred constraint
 * trigger checks at COMMIT that the entries of a transaction net to zero in every currency.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry extends BaseEntity {

    @Column(name = "transaction_uuid")
    private UUID transactionUuid;

    @Column(name = "account_uuid")
    private UUID accountUuid;

    @Convert(converter = LedgerEntryDirectionConverter.class)
    @Column(name = "direction")
    private LedgerEntryDirection direction;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_code")
    private String currencyCode;
}
