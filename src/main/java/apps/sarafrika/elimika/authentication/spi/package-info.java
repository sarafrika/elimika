/**
 * Keycloak Integration API for authentication module.
 * This package exposes identity and access management services that other modules can depend on.
 * Provides abstraction layer over Keycloak for user management and authentication operations.
 *
 * <p>Follows Spring Modulith named interface pattern for proper module encapsulation.
 * Other modules should reference this as: authentication :: keycloak-integration</p>
 */
@NamedInterface("keycloak-integration")
package apps.sarafrika.elimika.authentication.spi;

import org.springframework.modulith.NamedInterface;
