package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
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
 * Internal implementation of course security operations.
 * Provides authorization checks for course ownership.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
@Service("courseSecurityService")
@RequiredArgsConstructor
@Slf4j
public class CourseSecurityServiceImpl implements CourseSecuritySpi {

    private final CourseRepository courseRepository;
    private final CourseCreatorRepository courseCreatorRepository;
    private final UserRepository userRepository;

    /**
     * Checks if the currently authenticated user is the owner of the specified course.
     *
     * Flow: JWT (keycloakId) → User → CourseCreator → Course
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user owns the course, false otherwise
     */
    @Override
    public boolean isCourseOwner(UUID courseUuid) {
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

            // Find CourseCreator by User UUID
            CourseCreator courseCreator = courseCreatorRepository.findByUserUuid(user.getUuid()).orElse(null);
            if (courseCreator == null) {
                log.debug("CourseCreator not found for user UUID: {}", user.getUuid());
                return false;
            }

            // Get course and check ownership
            Course course = courseRepository.findByUuid(courseUuid).orElse(null);
            if (course == null) {
                log.debug("Course not found: {}", courseUuid);
                return false;
            }

            boolean isOwner = course.getCourseCreatorUuid() != null &&
                             course.getCourseCreatorUuid().equals(courseCreator.getUuid());

            log.debug("Course ownership check for user {} (courseCreator {}) on course {}: {}",
                     user.getUuid(), courseCreator.getUuid(), courseUuid, isOwner);

            return isOwner;

        } catch (Exception e) {
            log.error("Error checking course ownership for course: {}", courseUuid, e);
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
