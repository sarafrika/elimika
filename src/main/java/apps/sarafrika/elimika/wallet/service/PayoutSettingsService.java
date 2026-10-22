package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.entity.PayoutSettings;
import apps.sarafrika.elimika.wallet.enums.PayoutFeePayer;
import apps.sarafrika.elimika.wallet.repository.PayoutSettingsRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The withdrawal rules: limits, the review and second-approval thresholds, and who pays the M-Pesa charge. */
@Service
@RequiredArgsConstructor
public class PayoutSettingsService {

    private static final short ID = 1;

    private final PayoutSettingsRepository repository;

    @Transactional(readOnly = true)
    public PayoutSettings current() {
        return repository.findById(ID).orElseThrow(() -> new IllegalStateException("wallet_payout_settings is empty"));
    }

    @Transactional
    public PayoutSettings update(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal dailyLimit, BigDecimal reviewLimit,
                                 BigDecimal secondApprovalLimit, PayoutFeePayer feePayer, BigDecimal earnerFee,
                                 String updatedBy) {
        if (minAmount.signum() <= 0 || maxAmount.compareTo(minAmount) < 0 || dailyLimit.compareTo(maxAmount) < 0
                || reviewLimit.signum() <= 0 || secondApprovalLimit.signum() <= 0) {
            throw new IllegalArgumentException("Limits must be positive, with minimum ≤ maximum ≤ daily limit.");
        }
        BigDecimal fee = earnerFee == null ? BigDecimal.ZERO : earnerFee;
        if (fee.signum() < 0 || fee.compareTo(minAmount) >= 0) {
            throw new IllegalArgumentException("The earner fee must be zero or more and below the minimum withdrawal.");
        }
        PayoutSettings settings = current();
        settings.setMinAmount(minAmount);
        settings.setMaxAmount(maxAmount);
        settings.setDailyLimit(dailyLimit);
        settings.setReviewLimit(reviewLimit);
        settings.setSecondApprovalLimit(secondApprovalLimit);
        settings.setFeePayer(feePayer);
        settings.setEarnerFee(fee);
        settings.setUpdatedDate(OffsetDateTime.now(ZoneOffset.UTC));
        settings.setUpdatedBy(updatedBy);
        return repository.save(settings);
    }
}
