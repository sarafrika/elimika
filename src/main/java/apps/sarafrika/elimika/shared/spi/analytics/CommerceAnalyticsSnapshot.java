package apps.sarafrika.elimika.shared.spi.analytics;

/**
 * Snapshot of commerce analytics derived from purchase data.
 */
public record CommerceAnalyticsSnapshot(
        long totalOrders,
        long ordersLast30Days,
        long capturedOrders,
        long uniqueCustomers,
        long newCustomersLast30Days,
        long coursePurchasesLast30Days,
        long classPurchasesLast30Days
) {
}