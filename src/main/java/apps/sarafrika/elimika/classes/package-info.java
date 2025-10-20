/**
 * The Classes Module manages class templates and blueprints for the Elimika platform.
 * 
 * This module is responsible for defining "what" a class is, independent of "when" it occurs.
 * It follows Spring Modulith principles and provides:
 * 
 * - Class definition management (CRUD operations)
 * - Recurrence pattern configuration for repeating classes
 * - Resource association with class definitions
 * - Event-driven communication with other modules
 * 
 * Key Features:
 * - Class Definition: Create, update, and manage core class information
 * - Recurrence Rules: Define complex recurrence patterns for the Timetabling module
 * - Resource Management: Associate documents and links with class definitions
 * - Location Management: Support for online, in-person, and hybrid class formats
 * 
 * Module Boundaries:
 * - Owns: Class definitions, recurrence patterns, class resources
 * - Does Not Own: Actual scheduling, enrollments, instructor availability, attendance
 * 
 * The module exposes its services through well-defined SPIs while keeping implementation
 * details internal. Other modules can interact through events and service interfaces.
 * 
 * @since 2.9.2
 */
@ApplicationModule(
    allowedDependencies = {"shared", "availability :: availability-spi", "timetabling :: timetabling-spi"}
)
package apps.sarafrika.elimika.classes;

import org.springframework.modulith.ApplicationModule;