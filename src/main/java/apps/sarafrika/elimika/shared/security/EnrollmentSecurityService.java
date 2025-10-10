package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for enrollment-related authorization checks.
 * Provides methods to verify enrollment ownership and student identity.
 */
@Service("enrollmentSecurityService")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentSecurityService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * Checks if the currently authenticated user owns the specified enrollment.
     * This means checking if the enrollment belongs to a student associated with the current user.
     *
     * Flow: JWT (keycloakId) → User → Student → Enrollment
     *
     * @param enrollmentUuid UUID of the enrollment to check
     * @return true if the current user owns the enrollment, false otherwise
     */
    public boolean isOwner(UUID enrollmentUuid) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authenticated user found");
                return false;
            }

            // Get Keycloak ID from JWT
            String keycloakId = getKeycloakId(authentication);
            if (keycloakId == null) {
                log.debug("Could not extract Keycloak ID from authentication");
                return false;
            }

            // Find User by Keycloak ID
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find Student by User UUID
            Student student = studentRepository.findByUserUuid(user.getUuid()).orElse(null);
            if (student == null) {
                log.debug("Student not found for user UUID: {}", user.getUuid());
                return false;
            }

            // Get enrollment and check ownership
            Enrollment enrollment = enrollmentRepository.findByUuid(enrollmentUuid).orElse(null);
            if (enrollment == null) {
                log.debug("Enrollment not found: {}", enrollmentUuid);
                return false;
            }

            boolean isOwner = enrollment.getStudentUuid() != null &&
                             enrollment.getStudentUuid().equals(student.getUuid());

            log.debug("Enrollment ownership check for user {} (student {}) on enrollment {}: {}",
                     user.getUuid(), student.getUuid(), enrollmentUuid, isOwner);

            return isOwner;

        } catch (Exception e) {
            log.error("Error checking enrollment ownership for enrollment: {}", enrollmentUuid, e);
            return false;
        }
    }

    /**
     * Checks if the currently authenticated user is the student with the given UUID.
     *
     * Flow: JWT (keycloakId) → User → Student
     *
     * @param studentUuid UUID of the student to check
     * @return true if the current user is this student, false otherwise
     */
    public boolean isOwner(UUID studentUuid, String type) {
        // This overload is for checking student identity directly
        if (!"student".equalsIgnoreCase(type)) {
            return false;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authenticated user found");
                return false;
            }

            // Get Keycloak ID from JWT
            String keycloakId = getKeycloakId(authentication);
            if (keycloakId == null) {
                log.debug("Could not extract Keycloak ID from authentication");
                return false;
            }

            // Find User by Keycloak ID
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find Student by User UUID
            Student student = studentRepository.findByUserUuid(user.getUuid()).orElse(null);
            if (student == null) {
                log.debug("Student not found for user UUID: {}", user.getUuid());
                return false;
            }

            boolean isOwner = student.getUuid().equals(studentUuid);
            log.debug("Student identity check for user {} (student {}) against studentUuid {}: {}",
                     user.getUuid(), student.getUuid(), studentUuid, isOwner);

            return isOwner;

        } catch (Exception e) {
            log.error("Error checking student identity for studentUuid: {}", studentUuid, e);
            return false;
        }
    }

    /**
     * Extracts the Keycloak ID from the JWT token.
     *
     * @param authentication The authentication object
     * @return The Keycloak ID (sub claim), or null if not found
     */
    private String getKeycloakId(Authentication authentication) {
        try {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Error extracting Keycloak ID from JWT", e);
        }
        return null;
    }
}