package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Unified security service for domain-based authorization checks.
 * Replaces role-based authorization with domain-based checks.
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
 */
@Service("domainSecurityService")
@RequiredArgsConstructor
@Slf4j
public class DomainSecurityService {

    private final UserLookupService userLookupService;
    private final StudentLookupService studentLookupService;
    private final InstructorLookupService instructorLookupService;

    /**
     * Checks if the currently authenticated user is a student.
     */
    public boolean isStudent() {
        return hasUserDomain(UserDomain.student);
    }

    /**
     * Checks if the currently authenticated user is an instructor.
     */
    public boolean isInstructor() {
        return hasUserDomain(UserDomain.instructor);
    }

    /**
     * Checks if the currently authenticated user is an organization admin.
     */
    public boolean isOrganizationAdmin() {
        return hasUserDomain(UserDomain.admin);
    }

    /**
     * Checks if the currently authenticated user is a course creator.
     */
    public boolean isCourseCreator() {
        return hasUserDomain(UserDomain.course_creator);
    }

    /**
     * Checks if the current user has any of the specified domains.
     */
    public boolean hasAnyDomain(UserDomain... domains) {
        for (UserDomain domain : domains) {
            if (hasUserDomain(domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the current user is a student OR an instructor OR an org admin.
     * This is a common permission pattern in the system.
     */
    public boolean isStudentOrInstructorOrAdmin() {
        return hasAnyDomain(UserDomain.student, UserDomain.instructor, UserDomain.admin);
    }

    /**
     * Checks if the current user is an instructor OR an org admin.
     * This is a common permission pattern in the system.
     */
    public boolean isInstructorOrAdmin() {
        return hasAnyDomain(UserDomain.instructor, UserDomain.admin);
    }

    /**
     * Checks if the currently authenticated user belongs to a specific instructor.
     */
    public boolean isInstructorWithUuid(UUID instructorUuid) {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            if (currentUserUuid == null) {
                return false;
            }

            UUID instructorUserUuid = instructorLookupService.findInstructorUuidByUserUuid(currentUserUuid).orElse(null);
            if (instructorUserUuid == null) {
                return false;
            }

            return instructorUserUuid.equals(instructorUuid);
        } catch (Exception e) {
            log.error("Error checking instructor identity for instructorUuid: {}", instructorUuid, e);
            return false;
        }
    }

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     */
    public boolean isStudentWithUuid(UUID studentUuid) {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            if (currentUserUuid == null) {
                return false;
            }

            UUID studentUserUuid = studentLookupService.findStudentUuidByUserUuid(currentUserUuid).orElse(null);
            if (studentUserUuid == null) {
                return false;
            }

            return studentUserUuid.equals(studentUuid);
        } catch (Exception e) {
            log.error("Error checking student identity for studentUuid: {}", studentUuid, e);
            return false;
        }
    }

    /**
     * Gets the UUID of the current authenticated user.
     * Returns null if no user is authenticated.
     */
    public UUID getCurrentUserUuid() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String keycloakId = getKeycloakId(authentication);
            if (keycloakId == null) {
                return null;
            }

            return userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
        } catch (Exception e) {
            log.error("Error getting current user UUID", e);
            return null;
        }
    }

    /**
     * Checks if the current user has a specific user domain.
     */
    private boolean hasUserDomain(UserDomain domain) {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            if (currentUserUuid == null) {
                log.debug("No authenticated user found");
                return false;
            }

            boolean hasDomain = userLookupService.userHasDomain(currentUserUuid, domain);
            log.debug("User {} domain check for {}: {}", currentUserUuid, domain, hasDomain);
            return hasDomain;

        } catch (Exception e) {
            log.error("Error checking user domain: {}", domain, e);
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