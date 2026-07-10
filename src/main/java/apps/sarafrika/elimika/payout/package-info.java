/**
 * Payout module credits earner wallets (course creators and instructors) when a commerce order
 * is captured. It reacts to {@code OrderCompletedEvent}, resolves the earning party and their
 * revenue share per purchased item, and applies idempotent wallet credits.
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
