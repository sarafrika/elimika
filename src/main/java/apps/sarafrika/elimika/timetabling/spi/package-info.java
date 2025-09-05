/**
 * Service Provider Interface Package
 * <p>
 * This package defines the public API for the timetabling module that can be
 * consumed by other modules within the Elimika platform.
 * 
 * <h2>Service Interfaces</h2>
 * <ul>
 *   <li>{@link TimetableService} - Core timetabling operations for class scheduling and enrollment management</li>
 * </ul>
 * 
 * <p>These interfaces represent the module's contract with other modules and external clients.
 * They define all available operations for scheduling classes, managing enrollments, and
 * querying timetabling data.</p>
 * 
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@NamedInterface("timetabling-spi")
package apps.sarafrika.elimika.timetabling.spi;

import org.springframework.modulith.NamedInterface;