package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.utils.commerce.PlatformFeeApportionment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * How one sale line divides between the course creator and the party that delivered the class.
 * <p>
 * Each side takes its share of the gross and then pays the platform fee on that share, so the fee shows
 * on every earner's own statement. Remainder cents go to the delivery side, so the legs always add back
 * to exactly what the learner paid.
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

    /**
     * @param lineGross         what the learner paid for the line
     * @param creatorPercentage the course's creator share; the delivery side gets the rest
     * @param feeOnShare        the platform fee charged on one earner's gross share
     */
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
