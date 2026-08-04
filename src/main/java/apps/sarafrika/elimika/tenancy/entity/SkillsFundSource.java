package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A funding source contributing to an organisation's skills fund.
 * <p>
 * Removal is soft. A source is an input to a published balance — {@code remaining} is
 * {@code sum(sources) - sum(disbursed)} — so erasing one rewrites a number the organisation has
 * already seen and acted on, with nothing left to explain the change.
 */
@Entity
@Table(name = "skills_fund_sources")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkillsFundSource extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "amount")
    private BigDecimal amount;

    /** ISO-4217 code the {@link #amount} is denominated in. */
    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "deleted")
    private boolean deleted = false;
}
