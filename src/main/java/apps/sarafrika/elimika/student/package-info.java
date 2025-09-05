/**
 * The Student Module manages student profiles and academic information for the Elimika platform.
 * 
 * This module follows Spring Modulith principles and provides:
 * - Student profile management (CRUD operations)
 * - Student registration and onboarding workflows
 * - Integration with user management systems
 * - Event-driven communication with other modules
 * 
 * Key Features:
 * - Student Profile: Create, update, and manage student information
 * - User Domain Mapping: Automatic assignment of student role in the system
 * - Search and Filtering: Advanced query capabilities for student data
 * - Event Publishing: Domain events for cross-module integration
 * 
 * Module Boundaries:
 * - Owns: Student profiles, student-specific metadata
 * - Does Not Own: User authentication, course enrollments, class scheduling, grading
 * 
 * The module exposes its services through well-defined interfaces while keeping implementation
 * details internal. Other modules can interact through domain events and service interfaces.
 * 
 * Key Components:
 * - StudentService: Main service interface for student operations
 * - StudentDTO: Data transfer object for student information
 * - UserDomainMappingEvent: Events for user role assignment
 * - Student: Core entity representing student profiles
 * 
 * Integration Points:
 * - Publishes UserDomainMappingEvent when students are created
 * - Integrates with shared utilities for search and filtering
 * - Uses shared exception handling patterns
 * 
 * @since 1.0.0
 */
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.student;

import org.springframework.modulith.ApplicationModule;