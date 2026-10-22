package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "Withdrawal", description = "Money leaving a wallet for an M-Pesa number")
public record WithdrawalDTO(
        @JsonProperty("uuid") UUID uuid,
        @Schema(example = "WD-2026-000042") @JsonProperty("reference") String reference,
        @JsonProperty("status") WithdrawalStatus status,
        @Schema(description = "Taken from the wallet") @JsonProperty("amount") BigDecimal amount,
        @Schema(description = "M-Pesa charge paid by the earner; zero when Elimika pays it")
        @JsonProperty("fee_amount") BigDecimal feeAmount,
        @Schema(description = "What arrives on M-Pesa") @JsonProperty("net_amount") BigDecimal netAmount,
        @JsonProperty("currency_code") String currencyCode,
        @JsonProperty("payout_method_uuid") UUID payoutMethodUuid,
        @Schema(description = "The number, masked; the full number never leaves the server", example = "+254 7•• ••• 678")
        @JsonProperty("masked_msisdn") String maskedMsisdn,
        @JsonProperty("requested_by_user_uuid") UUID requestedByUserUuid,
        @Schema(description = "Why it waits for an admin, e.g. ABOVE_REVIEW_LIMIT, FIRST_PAYOUT_TO_NUMBER, NAME_MISMATCH")
        @JsonProperty("review_flags") List<String> reviewFlags,
        @JsonProperty("second_approver_user_uuid") UUID secondApproverUserUuid,
        @JsonProperty("rejection_reason") String rejectionReason,
        @JsonProperty("failure_reason") String failureReason,
        @JsonProperty("mpesa_receipt") String mpesaReceipt,
        @JsonProperty("mpesa_recipient_name") String mpesaRecipientName,
        @JsonProperty("created_date") LocalDateTime createdDate,
        @JsonProperty("confirmed_at") OffsetDateTime confirmedAt,
        @JsonProperty("sent_at") OffsetDateTime sentAt,
        @JsonProperty("completed_at") OffsetDateTime completedAt
) {

    public static WithdrawalDTO from(Withdrawal w) {
        return new WithdrawalDTO(w.getUuid(), w.getReference(), w.getStatus(), w.getAmount(), w.getFeeAmount(),
                w.getNetAmount(), w.getCurrencyCode(), w.getPayoutMethodUuid(), Msisdn.mask(w.getMsisdn()),
                w.getRequestedByUserUuid(), w.reviewFlagList(), w.getSecondApproverUserUuid(), w.getRejectionReason(),
                w.getFailureReason(), w.getMpesaReceipt(), w.getMpesaRecipientName(), w.getCreatedDate(),
                w.getConfirmedAt(), w.getSentAt(), w.getCompletedAt());
    }
}
