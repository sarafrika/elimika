package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.util.converter.WalletOwnerTypeConverter;
import apps.sarafrika.elimika.wallet.util.converter.WithdrawalStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Money leaving a wallet for an M-Pesa number, from request to M-Pesa's result. */
@Entity
@Table(name = "withdrawals")
@Getter
@Setter
@NoArgsConstructor
public class Withdrawal extends BaseEntity {

    @Column(name = "reference")
    private String reference;

    @Convert(converter = WalletOwnerTypeConverter.class)
    @Column(name = "owner_type")
    private WalletOwnerType ownerType;

    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Column(name = "payout_method_uuid")
    private UUID payoutMethodUuid;

    /** Copied from the payout number at request time, so the record stands if the number is removed. */
    @Column(name = "msisdn")
    private String msisdn;

    @Convert(converter = WithdrawalStatusConverter.class)
    @Column(name = "status")
    private WithdrawalStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "requested_by_user_uuid")
    private UUID requestedByUserUuid;

    @Column(name = "requester_name")
    private String requesterName;

    /** Comma-separated reasons this withdrawal went to review. */
    @Column(name = "review_flags")
    private String reviewFlags;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "second_approver_user_uuid")
    private UUID secondApproverUserUuid;

    @Column(name = "second_approved_at")
    private OffsetDateTime secondApprovedAt;

    @Column(name = "reviewed_by_user_uuid")
    private UUID reviewedByUserUuid;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "gateway_payout_uuid")
    private String gatewayPayoutUuid;

    @Column(name = "mpesa_receipt")
    private String mpesaReceipt;

    @Column(name = "mpesa_recipient_name")
    private String mpesaRecipientName;

    @Column(name = "mpesa_charges")
    private BigDecimal mpesaCharges;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "status_checks")
    private int statusChecks;

    public WalletOwner owner() {
        return new WalletOwner(ownerType, ownerUuid);
    }

    public List<String> reviewFlagList() {
        return reviewFlags == null || reviewFlags.isBlank() ? List.of() : Arrays.asList(reviewFlags.split(","));
    }
}
