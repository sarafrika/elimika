/**
 * Shared Module - Common Utilities and Cross-Cutting Concerns
 *
 * This is an OPEN module accessible to all other modules in the application.
 * Contains shared utilities, exceptions, events, security components, and storage services.
 *
 * <p>Module Type: OPEN - All packages are accessible to other modules</p>
 *
 * <p>Module Dependencies:</p>
 * <ul>
 *   <li>authentication :: keycloak-integration - For Keycloak user synchronization</li>
 *   <li>tenancy - For user and organization management services</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {"authentication :: keycloak-integration", "tenancy"}
)
package apps.sarafrika.elimika.shared;

import org.springframework.modulith.ApplicationModule;