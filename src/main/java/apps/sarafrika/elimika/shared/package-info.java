/**
 * Shared Module - Common Utilities and Cross-Cutting Concerns
 *
 * This is an OPEN module accessible to all other modules in the application.
 * Contains shared utilities, exceptions, events, security components, storage services,
 * and shared SPIs (analytics, timetabling).
 *
 * <p>Module Type: OPEN - All packages are accessible to other modules</p>
 *
 * <p>Module Dependencies:</p>
 * <ul>
 *   <li>authentication :: keycloak-integration - For Keycloak user synchronization</li>
 *   <li>tenancy :: tenancy-spi - For user and domain lookup services</li>
 *   <li>student :: student-spi - For student lookup services</li>
 *   <li>instructor :: instructor-spi - For instructor lookup services</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {
                "authentication :: keycloak-integration",
                "tenancy :: tenancy-spi",
                "student :: student-spi",
                "instructor :: instructor-spi"
        }
)
package apps.sarafrika.elimika.shared;

import org.springframework.modulith.ApplicationModule;