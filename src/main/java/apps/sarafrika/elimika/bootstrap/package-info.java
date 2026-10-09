/**
 * Session bootstrap: one read that composes identity, role profiles, the active organisation,
 * wallet balance and notification badges so a dashboard shell can render from a single request.
 */
@ApplicationModule(
        allowedDependencies = {
                "shared",
                "tenancy :: tenancy-spi",
                "tenancy :: user-dto",
                "student :: student-spi",
                "instructor :: instructor-spi",
                "coursecreator :: coursecreator-spi",
                "wallet :: wallet-spi",
                "notifications :: notifications-spi"
        }
)
package apps.sarafrika.elimika.bootstrap;

import org.springframework.modulith.ApplicationModule;
