package apps.sarafrika.elimika.student.internal.security;

import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.student.spi.StudentSecuritySpi;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Internal implementation of student security operations.
 * Provides authorization checks for student identity.
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
 *
 * @author Wilfred Njuguna
 * @version 1.1
 * @since 2025-10-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentSecurityServiceImpl implements StudentSecuritySpi {

    private final StudentLookupService studentLookupService;
    private final UserLookupService userLookupService;

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     *
     * Flow: JWT (keycloakId) → User → Student
     *
     * @param studentUuid UUID of the student to check
     * @return true if the current user is the specified student, false otherwise
     */
    @Override
    public boolean isStudentWithUuid(UUID studentUuid) {
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

            // Find User UUID by Keycloak ID via SPI
            UUID userUuid = userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            if (userUuid == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find Student UUID by User UUID via SPI
            UUID currentStudentUuid = studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null);
            if (currentStudentUuid == null) {
                log.debug("Student not found for user UUID: {}", userUuid);
                return false;
            }

            boolean isStudent = currentStudentUuid.equals(studentUuid);
            log.debug("Student identity check for user {} (student {}) against target {}: {}",
                     userUuid, currentStudentUuid, studentUuid, isStudent);

            return isStudent;
        } catch (Exception e) {
            log.error("Error checking student identity for studentUuid: {}", studentUuid, e);
            return false;
        }
    }

    /**
     * Extracts the Keycloak ID from the JWT token.
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
