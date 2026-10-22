package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import apps.sarafrika.elimika.wallet.util.converter.PayoutFeePayerConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The one row of withdrawal rules admins set. */
@Entity
@Table(name = "wallet_payout_settings")
@Getter
@Setter
@NoArgsConstructor
public class PayoutSettings {

    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    /** Above this, a withdrawal waits for an admin. */
    @Column(name = "review_limit")
    private BigDecimal reviewLimit;

    /** Above this, an organisation withdrawal needs a second payout approver. */
    @Column(name = "second_approval_limit")
    private BigDecimal secondApprovalLimit;

    @Convert(converter = PayoutFeePayerConverter.class)
    @Column(name = "fee_payer")
    private PayoutFeePayer feePayer;

    /** Charged to the earner per withdrawal when they pay the M-Pesa charge. */
    @Column(name = "earner_fee")
    private BigDecimal earnerFee;

    @Column(name = "updated_date")
    private OffsetDateTime updatedDate;

    @Column(name = "updated_by")
    private String updatedBy;
}
