package apps.sarafrika.elimika.payout.model;

import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.util.converter.InstructorObligationStatusConverter;
import apps.sarafrika.elimika.shared.model.BaseEntity;
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
 * What an organisation owes an instructor for one delivered session.
 * <p>
 * This is the record that replaces a number recomputed on every page load. The distinction that
 * matters is {@link #rateAmount}: it is written once, at accrual, from the training fee that stood
 * on the day the session completed, and is never recalculated. Re-rating a class therefore changes
 * what future sessions are worth and leaves everything already earned untouched — which the old
 * {@code training_fee x completed_sessions} expression could not do, because it always multiplied
 * by today's rate.
 * <p>
 * Settlement is off-platform. The organisation pays by its own means and records the fact here with
 * its own reference; no money moves through a wallet.
 */
@Entity
@Table(name = "instructor_obligations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorObligation extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "instructor_user_uuid")
    private UUID instructorUserUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "session_uuid")
    private UUID sessionUuid;

    /** The per-session fee as it stood at accrual. Never recomputed. */
    @Column(name = "rate_amount")
    private BigDecimal rateAmount;

    @Column(name = "currency_code")
    private String currencyCode;

    @Convert(converter = InstructorObligationStatusConverter.class)
    @Column(name = "status")
    private InstructorObligationStatus status;

    @Column(name = "accrued_at")
    private LocalDateTime accruedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "settlement_reference")
    private String settlementReference;

    @Column(name = "settled_by")
    private String settledBy;

    @Column(name = "status_note")
    private String statusNote;
}
