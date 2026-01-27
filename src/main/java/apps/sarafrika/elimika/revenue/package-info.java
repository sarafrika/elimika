/**
 * Revenue module consolidates sales analytics for domain-specific dashboards.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared",
                "commerce.purchase :: commerce-purchase-spi",
                "commerce :: commerce-internal-spi",
                "course :: course-spi",
                "classes :: classes-spi",
                "instructor :: instructor-spi",
                "coursecreator :: coursecreator-spi",
                "tenancy :: tenancy-spi",
                "student :: student-spi"
        }
)
package apps.sarafrika.elimika.revenue;
