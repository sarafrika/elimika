/**
 * The Instructor Module manages instructor profiles, qualifications, and professional information for the Elimika platform.
 * 
 * This module follows Spring Modulith principles and provides:
 * - Instructor profile management with comprehensive professional details
 * - Qualification and certification tracking
 * - Professional experience and education history
 * - Document management for instructor credentials
 * - Admin verification workflows for instructor approval
 * - Event-driven communication with other modules
 * 
 * Key Features:
 * - Instructor Profiles: Create, update, and manage detailed instructor information
 * - Professional History: Track education, experience, skills, and memberships
 * - Document Management: Upload and manage professional documents and certificates
 * - Verification System: Admin approval workflow for instructor registration
 * - User Domain Mapping: Automatic assignment of instructor role in the system
 * - Search and Filtering: Advanced query capabilities for instructor data
 * 
 * Module Boundaries:
 * - Owns: Instructor profiles, qualifications, professional documents, verification status
 * - Does Not Own: User authentication, class scheduling, availability management, course assignments
 * 
 * The module exposes its services through well-defined interfaces while keeping implementation
 * details internal. Other modules can interact through domain events and service interfaces.
 * 
 * Key Components:
 * - InstructorService: Main service interface for instructor operations
 * - InstructorDTO: Data transfer object for instructor information
 * - InstructorDocument: Management of instructor credentials and certificates
 * - InstructorEducation: Educational background tracking
 * - InstructorExperience: Professional experience management
 * - InstructorSkill: Skills and competencies tracking
 * - InstructorProfessionalMembership: Professional organization memberships
 * - UserDomainMappingEvent: Events for user role assignment
 * 
 * Integration Points:
 * - Publishes UserDomainMappingEvent when instructors are created
 * - Integrates with shared utilities for search, filtering, and document handling
 * - Uses shared exception handling patterns
 * - Potential integration with availability module for instructor scheduling
 * 
 * @since 1.0.0
 */
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.instructor;

import org.springframework.modulith.ApplicationModule;