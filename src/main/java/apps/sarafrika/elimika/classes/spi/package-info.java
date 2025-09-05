/**
 * Service Provider Interface (SPI) package for the Classes module.
 * 
 * This package contains the public API interfaces that other modules can use
 * to interact with the Classes module. It follows Spring Modulith
 * named interface patterns for clean module boundaries.
 * 
 * The SPI provides:
 * - ClassDefinitionService: Main service interface for class definition operations
 * - Event DTOs: For domain event communication with other modules
 * 
 * @since 2.9.2
 */
@NamedInterface("classes-spi")
package apps.sarafrika.elimika.classes.spi;

import org.springframework.modulith.NamedInterface;