package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A balanced movement of money. Immutable once written - the database refuses UPDATE and DELETE on
 * this table, so a mistake is corrected by posting a reversing transaction, never by editing away
 * the evidence.
 */
@Entity
@Table(name = "ledger_transactions")
@Getter
@Setter
@NoArgsConstructor
public class LedgerTransaction extends BaseEntity {

    /** Unique. Replaying the same business event re-derives this key and is rejected as a duplicate. */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "description")
    private String description;

    /** What caused this posting, as a free label. The ledger does not interpret it. */
    @Column(name = "cause_type")
    private String causeType;

    @Column(name = "cause_uuid")
    private UUID causeUuid;
}
