package apps.sarafrika.elimika.student.internal.security;

import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.spi.StudentSecuritySpi;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
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
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentSecurityServiceImpl implements StudentSecuritySpi {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     *
     * @param studentUuid UUID of the student to check
     * @return true if the current user is the specified student, false otherwise
     */
    @Override
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
    private User getCurrentUser() {
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
