package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
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
 * Internal implementation of course security operations.
 * Provides authorization checks for course ownership.
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
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
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;

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

            // Find User by Keycloak ID via SPI
            UUID userUuid = userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            if (userUuid == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find CourseCreator by User UUID via SPI
            UUID courseCreatorUuid = courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).orElse(null);
            if (courseCreatorUuid == null) {
                log.debug("CourseCreator not found for user UUID: {}", userUuid);
                return false;
            }

            // Get course and check ownership
            Course course = courseRepository.findByUuid(courseUuid).orElse(null);
            if (course == null) {
                log.debug("Course not found: {}", courseUuid);
                return false;
            }

            boolean isOwner = course.getCourseCreatorUuid() != null &&
                             course.getCourseCreatorUuid().equals(courseCreatorUuid);

            log.debug("Course ownership check for user {} (courseCreator {}) on course {}: {}",
                     userUuid, courseCreatorUuid, courseUuid, isOwner);

            return isOwner;

        } catch (Exception e) {
            log.error("Error checking course ownership for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * Grants content read access to the course owner or a member of an organisation
     * approved to train the course. Admins are handled at the endpoint; enrolled
     * learners use their own class flow, not this path.
     */
    @Override
    public boolean canReadCourseContent(UUID courseUuid) {
        if (isCourseOwner(courseUuid)) {
            return true;
        }
        try {
            UUID userUuid = currentUserUuid();
            if (userUuid == null) {
                return false;
            }
            return belongsToApprovedTrainingOrganisation(courseUuid, userUuid);
        } catch (Exception e) {
            log.error("Error checking content read access for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * Grants gradebook access only where there is a real relationship to the course.
     * <p>
     * Deliberately stricter than a role check: holding the instructor or course_creator
     * domain says nothing about whether this particular course is yours to mark.
     */
    @Override
    public boolean canManageCourseGradebook(UUID courseUuid) {
        if (isCourseOwner(courseUuid)) {
            return true;
        }
        try {
            UUID userUuid = currentUserUuid();
            if (userUuid == null) {
                return false;
            }

            UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(userUuid).orElse(null);
            if (instructorUuid != null && courseTrainingApplicationRepository
                    .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                            courseUuid,
                            CourseTrainingApplicantType.INSTRUCTOR,
                            instructorUuid,
                            CourseTrainingApplicationStatus.APPROVED)) {
                return true;
            }

            return belongsToApprovedTrainingOrganisation(courseUuid, userUuid);
        } catch (Exception e) {
            log.error("Error checking gradebook access for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * True when the user belongs to an organisation approved to train this course.
     */
    private boolean belongsToApprovedTrainingOrganisation(UUID courseUuid, UUID userUuid) {
        List<UUID> organisationUuids = userLookupService.getUserOrganizations(userUuid);
        for (UUID organisationUuid : organisationUuids) {
            boolean approved = courseTrainingApplicationRepository
                    .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                            courseUuid,
                            CourseTrainingApplicantType.ORGANISATION,
                            organisationUuid,
                            CourseTrainingApplicationStatus.APPROVED);
            if (approved) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the current authenticated user's UUID from the JWT, or null.
     */
    private UUID currentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String keycloakId = getKeycloakId(authentication);
        if (keycloakId == null) {
            return null;
        }
        return userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
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
