/**
 * The Course Creator Module manages users whose sole mandate is creating educational content.
 *
 * This module follows Spring Modulith principles and handles course creator profiles and their
 * relationship with courses. Course creators are distinct from instructors - they focus on
 * content creation rather than instruction delivery.
 *
 * Key Features:
 * - Course creator profile management
 * - Admin verification workflow for course creators
 * - Association between course creators and their created courses
 *
 * Module Boundaries:
 * - Owns: Course creator profiles, verification status
 * - Does Not Own: Courses (owned by course module), instruction delivery
 *
 * Integration Points:
 * - Course module uses course creator UUIDs for course ownership
 * - Authentication module for user identity
 * - Admin verification similar to instructor/organization verification
 *
 * @since 2.17.0
 */
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.coursecreator;

import org.springframework.modulith.ApplicationModule;