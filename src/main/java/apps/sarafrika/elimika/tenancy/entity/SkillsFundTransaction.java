package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.tenancy.util.converter.SkillsFundTransactionStatusConverter;
import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A movement within an organisation's skills fund (allocation, disbursement, adjustment).
 * <p>
 * Two fields carry the weight of this being money rather than a report: {@link #currencyCode}, so an
 * amount means something on its own, and {@link #beneficiaryUserUuid}, so a disbursement names the
 * person it is for. {@link #targetName} remains for display only and must never be used to resolve a
 * recipient.
 */
@Entity
@Table(name = "skills_fund_transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkillsFundTransaction extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "description")
    private String description;

    /** Display label for the recipient. Not an identity — see {@link #beneficiaryUserUuid}. */
    @Column(name = "target_name")
    private String targetName;

    /**
     * The platform user this movement is for. Null on rows written before the column existed, and on
     * movements that genuinely have no individual recipient.
     */
    @Column(name = "beneficiary_user_uuid")
    private UUID beneficiaryUserUuid;

    @Column(name = "amount")
    private BigDecimal amount;

    /** ISO-4217 code the {@link #amount} is denominated in. */
    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "transaction_type")
    private String transactionType;

    @Convert(converter = SkillsFundTransactionStatusConverter.class)
    @Column(name = "status")
    private SkillsFundTransactionStatus status;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
}
