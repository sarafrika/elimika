package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.utils.commerce.PlatformFeeApportionment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * One sale line split between the course creator and the delivery party; each pays the platform fee
 * on their own share. Remainder cents go to delivery, so the legs always add back to the gross.
 */
public record RevenueSplit(Leg creator, Leg delivery) {

    private static final int SCALE = PlatformFeeApportionment.MONEY_SCALE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** One earner's part of the line: their gross share, the fee charged on it and what they receive. */
    public record Leg(BigDecimal gross, BigDecimal platformFee, BigDecimal net) {

        static Leg of(BigDecimal gross, UnaryOperator<BigDecimal> feeOnShare) {
            BigDecimal fee = gross.signum() <= 0 ? zero() : Objects.requireNonNullElse(feeOnShare.apply(gross), zero());
            fee = fee.max(zero()).min(gross).setScale(SCALE, RoundingMode.HALF_UP);
            return new Leg(gross, fee, gross.subtract(fee));
        }

        public boolean earnsAnything() {
            return gross.signum() > 0;
        }
    }

    /** Splits {@code lineGross} by the creator's percentage; {@code feeOnShare} prices one earner's fee. */
    public static RevenueSplit of(BigDecimal lineGross, BigDecimal creatorPercentage, UnaryOperator<BigDecimal> feeOnShare) {
        BigDecimal gross = lineGross == null || lineGross.signum() <= 0
                ? zero()
                : lineGross.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal creatorGross = gross.multiply(creatorPercentage)
                .divide(HUNDRED, SCALE, RoundingMode.DOWN)
                .max(zero())
                .min(gross);
        BigDecimal deliveryGross = gross.subtract(creatorGross);
        return new RevenueSplit(Leg.of(creatorGross, feeOnShare), Leg.of(deliveryGross, feeOnShare));
    }

    public BigDecimal totalPlatformFee() {
        return creator.platformFee().add(delivery.platformFee());
    }

    public BigDecimal totalNet() {
        return creator.net().add(delivery.net());
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
