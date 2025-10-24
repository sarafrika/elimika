package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
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
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
 */
@Service("enrollmentSecurityService")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentSecurityService {

    private final EnrollmentLookupService enrollmentLookupService;
    private final StudentLookupService studentLookupService;
    private final UserLookupService userLookupService;

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
            UUID userUuid = userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            if (userUuid == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find Student by User UUID
            UUID studentUuid = studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null);
            if (studentUuid == null) {
                log.debug("Student not found for user UUID: {}", userUuid);
                return false;
            }

            // Get enrollment student UUID and check ownership
            UUID enrollmentStudentUuid = enrollmentLookupService.getEnrollmentStudentUuid(enrollmentUuid).orElse(null);
            if (enrollmentStudentUuid == null) {
                log.debug("Enrollment not found: {}", enrollmentUuid);
                return false;
            }

            boolean isOwner = enrollmentStudentUuid.equals(studentUuid);

            log.debug("Enrollment ownership check for user {} (student {}) on enrollment {}: {}",
                     userUuid, studentUuid, enrollmentUuid, isOwner);

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
            UUID userUuid = userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            if (userUuid == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find Student by User UUID
            UUID currentStudentUuid = studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null);
            if (currentStudentUuid == null) {
                log.debug("Student not found for user UUID: {}", userUuid);
                return false;
            }

            boolean isOwner = currentStudentUuid.equals(studentUuid);
            log.debug("Student identity check for user {} (student {}) against studentUuid {}: {}",
                     userUuid, currentStudentUuid, studentUuid, isOwner);

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