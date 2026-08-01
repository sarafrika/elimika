/**
 * Payout module credits earner wallets (course creators and instructors) when a commerce order
 * is captured. It reacts to {@code OrderCompletedEvent}, resolves the earning party and their
 * revenue share per purchased item, and applies idempotent wallet credits.
 * <p>
 * Crediting is a persistent Spring Modulith event listener, so a credit that fails is left as an
 * incomplete row in {@code event_publication} rather than lost, and is retried on a schedule and on
 * restart. Credits never run on the checkout thread, so a payout failure cannot fail an order.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Payout",
        allowedDependencies = {
                "shared",
                "course::course-spi",
                "instructor::instructor-spi",
                "wallet::wallet-spi"
        }
)
package apps.sarafrika.elimika.payout;
