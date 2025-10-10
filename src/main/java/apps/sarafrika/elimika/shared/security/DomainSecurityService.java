package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
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
 */
@Service("domainSecurityService")
@RequiredArgsConstructor
@Slf4j
public class DomainSecurityService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserDomainRepository userDomainRepository;

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
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return false;
            }

            Instructor instructor = instructorRepository.findByUserUuid(currentUser.getUuid()).orElse(null);
            if (instructor == null) {
                return false;
            }

            return instructor.getUuid().equals(instructorUuid);
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
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return false;
            }

            Student student = studentRepository.findByUserUuid(currentUser.getUuid()).orElse(null);
            if (student == null) {
                return false;
            }

            return student.getUuid().equals(studentUuid);
        } catch (Exception e) {
            log.error("Error checking student identity for studentUuid: {}", studentUuid, e);
            return false;
        }
    }

    /**
     * Gets the current authenticated user.
     */
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            String keycloakId = getKeycloakId(authentication);
            if (keycloakId == null) {
                return null;
            }

            return userRepository.findByKeycloakId(keycloakId).orElse(null);
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return null;
        }
    }

    /**
     * Gets the UUID of the current authenticated user.
     */
    public UUID getCurrentUserUuid() {
        User user = getCurrentUser();
        return user != null ? user.getUuid() : null;
    }

    /**
     * Checks if the current user has a specific user domain.
     */
    private boolean hasUserDomain(UserDomain domain) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                log.debug("No authenticated user found");
                return false;
            }

            // Get the domain entity from the database by domain name (enum value)
            apps.sarafrika.elimika.tenancy.entity.UserDomain domainEntity = userDomainRepository
                    .findByDomainName(domain.name())
                    .orElse(null);

            if (domainEntity == null) {
                log.warn("Domain {} not found in database", domain.name());
                return false;
            }

            // Get user domain mappings from repository
            List<UserDomainMapping> userDomainMappings = userDomainMappingRepository.findByUserUuid(currentUser.getUuid());
            if (userDomainMappings == null || userDomainMappings.isEmpty()) {
                log.debug("User {} has no domain mappings", currentUser.getUuid());
                return false;
            }

            // Check if the user has the specific domain by comparing UUIDs
            UUID domainUuid = domainEntity.getUuid();
            boolean hasDomain = userDomainMappings.stream()
                    .anyMatch(mapping -> domainUuid.equals(mapping.getUserDomainUuid()));

            log.debug("User {} domain check for {}: {}", currentUser.getUuid(), domain, hasDomain);
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