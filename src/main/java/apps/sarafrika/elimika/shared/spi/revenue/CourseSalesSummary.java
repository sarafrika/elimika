package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;

/**
 * What one course has sold, aggregated in the database.
 * <p>
 * Deliberately four totals and nothing else. The commerce line items behind them carry the buyer,
 * the learner and the order number, and none of that belongs in a course's statistics block — so
 * the aggregation happens in SQL and the identities never leave the commerce module.
 *
 * @param grossSales     captured line totals for the course
 * @param platformFee    the platform's share of those lines, taken off the top
 * @param paidOrders     distinct captured orders containing the course
 * @param refundedOrders distinct orders containing the course that were refunded, wholly or partly
 */
public record CourseSalesSummary(
        BigDecimal grossSales,
        BigDecimal platformFee,
        long paidOrders,
        long refundedOrders
) {

    public static CourseSalesSummary empty() {
        return new CourseSalesSummary(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
    }
}
