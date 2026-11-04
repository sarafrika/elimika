package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * How one sale line was split between the course creator and the delivery party, as settled at capture:
 * each leg's owner, gross, fee and net, the creator's percentage and the fee rule that priced the fees.
 */
public record SaleSplitDetail(
        BigDecimal creatorPercentage,
        Leg creator,
        SaleDeliveryKind deliveryKind,
        Leg delivery,
        FeeRule feeRule,
        OffsetDateTime settledAt
) {

    public SaleSplitDetail {
        Objects.requireNonNull(creator, "creator leg is required");
        Objects.requireNonNull(delivery, "delivery leg is required");
    }

    /** One party's leg; amounts are zero when it was not paid, and the owner is null when nobody was resolved. */
    public record Leg(SalePartyType ownerType, UUID ownerUuid, SaleLegStatus status,
                      BigDecimal gross, BigDecimal fee, BigDecimal net) {

        public Leg {
            Objects.requireNonNull(status, "status is required");
            gross = Objects.requireNonNullElse(gross, BigDecimal.ZERO);
            fee = Objects.requireNonNullElse(fee, BigDecimal.ZERO);
            net = Objects.requireNonNullElse(net, BigDecimal.ZERO);
        }

        public static Leg unpaid(SalePartyType ownerType, UUID ownerUuid, SaleLegStatus status) {
            return new Leg(ownerType, ownerUuid, status, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public boolean paid() {
            return status == SaleLegStatus.PAID;
        }
    }

    /** The platform fee rule in force when the order was placed; all null when there was none. */
    public record FeeRule(UUID ruleUuid, String ruleKey, SaleFeeMode mode, BigDecimal rate, String rateCurrency,
                          Boolean waived, BigDecimal discountPercentage, BigDecimal discountAmount,
                          OffsetDateTime evaluatedAt) {
    }

    /**
     * Whether the legs explain the line's settlement: each leg's fee + net is its gross, the legs' fees are
     * the line's fee, the legs' nets are what was credited and the creator's net is the creator's credit.
     */
    public boolean reconcilesWith(PurchaseLineSettlement settlement) {
        return same(creator.fee().add(creator.net()), creator.gross())
                && same(delivery.fee().add(delivery.net()), delivery.gross())
                && same(creator.fee().add(delivery.fee()), settlement.platformFeeAmount())
                && same(creator.net().add(delivery.net()), settlement.creditedAmount())
                && same(creator.net(), settlement.creatorCreditedAmount());
    }

    private static boolean same(BigDecimal left, BigDecimal right) {
        return right != null && left.compareTo(right) == 0;
    }
}
