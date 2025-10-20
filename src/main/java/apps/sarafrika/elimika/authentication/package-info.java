/**
 * Authentication Module - Keycloak Integration
 *
 * This module provides identity and access management integration with Keycloak.
 * It handles user authentication, role management, and session management through Keycloak.
 *
 * <p>Named Interfaces:</p>
 * <ul>
 *   <li>keycloak-integration: Public API for Keycloak user and role management operations</li>
 * </ul>
 *
 * <p>Module Dependencies:</p>
 * <ul>
 *   <li>shared - Events, exceptions, and common utilities (OPEN module)</li>
 * </ul>
 */
@ApplicationModule(
        allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.authentication;

import org.springframework.modulith.ApplicationModule;