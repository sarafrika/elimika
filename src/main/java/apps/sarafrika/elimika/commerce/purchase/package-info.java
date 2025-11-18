/**
 * Commerce Purchase module responsible for persisting order snapshots received from Medusa.
 * Exposes analytics and purchase state via named interfaces while keeping persistence internal.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "commerce :: commerce-paywall",
                "tenancy :: tenancy-spi",
                "student :: student-spi"
        }
)
package apps.sarafrika.elimika.commerce.purchase;
