/**
 * Service Provider Interface (SPI) package for the Availability module.
 * 
 * This package contains the public API interfaces that other modules can use
 * to interact with the Availability module. It follows Spring Modulith
 * named interface patterns for clean module boundaries.
 * 
 * The SPI provides:
 * - AvailabilityService: Main service interface for availability operations
 * - Event DTOs: For domain event communication with other modules
 * 
 * @since 2.11.0
 */
@NamedInterface("availability-spi")
package apps.sarafrika.elimika.availability.spi;

import org.springframework.modulith.NamedInterface;