package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.util.converter.PayoutBatchStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Withdrawals one admin gathers and submits, and a different admin approves before any is paid. */
@Entity
@Table(name = "payout_batches")
@Getter
@Setter
@NoArgsConstructor
public class PayoutBatch extends BaseEntity {

    @Convert(converter = PayoutBatchStatusConverter.class)
    @Column(name = "status")
    private PayoutBatchStatus status;

    @Column(name = "created_by_user_uuid")
    private UUID createdByUserUuid;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "submitted_by_user_uuid")
    private UUID submittedByUserUuid;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "approved_by_user_uuid")
    private UUID approvedByUserUuid;

    @Column(name = "approved_by_name")
    private String approvedByName;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_by_user_uuid")
    private UUID rejectedByUserUuid;

    @Column(name = "rejected_by_name")
    private String rejectedByName;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Rows in the batch; kept as it was when a rejected or cancelled batch let its rows go. */
    @Column(name = "row_count")
    private int rowCount;

    /** Sum of the rows' net amounts: what M-Pesa pays out. */
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    /** Utility account funds M-Pesa reported when the batch was approved; null when unknown. */
    @Column(name = "float_at_approval")
    private BigDecimal floatAtApproval;

    /** True when the approver released the batch while the float was unknown. */
    @Column(name = "float_override")
    private boolean floatOverride;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /** Whether this user may approve the batch now: it waits for approval and they neither built nor submitted it. */
    public boolean canBeApprovedBy(UUID userUuid) {
        return status == PayoutBatchStatus.AWAITING_APPROVAL && userUuid != null
                && !userUuid.equals(createdByUserUuid) && !userUuid.equals(submittedByUserUuid);
    }
}
