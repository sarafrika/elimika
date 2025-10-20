/**
 * Availability Module - Instructor Availability and Scheduling Management
 *
 * This module manages instructor availability, time blocks, and scheduling constraints.
 * It provides services to check instructor availability for class scheduling.
 *
 * <p>Named Interfaces:</p>
 * <ul>
 *   <li>availability-spi: Public API for availability operations</li>
 * </ul>
 *
 * <p>Module Dependencies:</p>
 * <ul>
 *   <li>shared - Common utilities and events</li>
 * </ul>
 *
 * <p>Note: This module listens to class events but does NOT have a direct dependency on classes module</p>
 */
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.availability;

import org.springframework.modulith.ApplicationModule;