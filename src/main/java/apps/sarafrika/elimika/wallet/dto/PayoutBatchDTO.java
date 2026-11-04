package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService.RowCounts;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Schema(name = "PayoutBatch", description = "Withdrawals paid together once a second admin approves them")
public record PayoutBatchDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("status") PayoutBatchStatus status,
        @Schema(description = "User who built the batch") @JsonProperty("created_by") UUID createdBy,
        @JsonProperty("created_by_name") String createdByName,
        @JsonProperty("submitted_by") UUID submittedBy,
        @JsonProperty("submitted_by_name") String submittedByName,
        @JsonProperty("submitted_at") OffsetDateTime submittedAt,
        @JsonProperty("approved_by") UUID approvedBy,
        @JsonProperty("approved_by_name") String approvedByName,
        @JsonProperty("approved_at") OffsetDateTime approvedAt,
        @JsonProperty("rejected_by") UUID rejectedBy,
        @JsonProperty("rejected_at") OffsetDateTime rejectedAt,
        @JsonProperty("rejection_reason") String rejectionReason,
        @Schema(description = "Rows in the batch; for a rejected or cancelled batch, the rows it had when it let them go")
        @JsonProperty("row_count") int rowCount,
        @Schema(description = "Sum of the rows' net amounts: what M-Pesa pays out")
        @JsonProperty("total_amount") BigDecimal totalAmount,
        @JsonProperty("paid_count") long paidCount,
        @JsonProperty("failed_count") long failedCount,
        @Schema(description = "Rows being handed to or waiting on M-Pesa (SENDING or PROCESSING)")
        @JsonProperty("in_flight_count") long inFlightCount,
        @Schema(description = "Utility account funds M-Pesa reported at approval; null when unknown and overridden")
        @JsonProperty("float_at_approval") BigDecimal floatAtApproval,
        @JsonProperty("released_at") OffsetDateTime releasedAt,
        @JsonProperty("completed_at") OffsetDateTime completedAt,
        @JsonProperty("created_date") OffsetDateTime createdDate,
        @JsonProperty("updated_date") OffsetDateTime updatedDate,
        @Schema(description = "True when the signed-in admin may approve the batch now: it waits for approval and "
                + "they neither built nor submitted it")
        @JsonProperty("can_approve") boolean canApprove
) {

    public static PayoutBatchDTO from(PayoutBatch b, RowCounts counts, UUID currentUser) {
        RowCounts c = counts == null ? RowCounts.NONE : counts;
        return new PayoutBatchDTO(b.getUuid(), b.getStatus(), b.getCreatedByUserUuid(), b.getCreatedByName(),
                b.getSubmittedByUserUuid(), b.getSubmittedByName(), b.getSubmittedAt(), b.getApprovedByUserUuid(),
                b.getApprovedByName(), b.getApprovedAt(), b.getRejectedByUserUuid(), b.getRejectedAt(),
                b.getRejectionReason(), b.getRowCount(), b.getTotalAmount(), c.paid(), c.failed(), c.inFlight(),
                b.getFloatAtApproval(), b.getReleasedAt(), b.getCompletedAt(), utc(b.getCreatedDate()),
                utc(b.getLastModifiedDate()), b.canBeApprovedBy(currentUser));
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
