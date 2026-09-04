package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramRequirement;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramRequirementRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final String CACHE_ENROLLED_COURSES = "courseSecurity.enrolledCourses";
    private static final String CACHE_OWNED_PROGRAM_PREFIX = "courseSecurity.ownsProgram.";

    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final UserLookupService userLookupService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final ProgramRequirementRepository programRequirementRepository;

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
     * Checks if the currently authenticated user created the specified training program.
     * <p>
     * A program is owned the way a course is, through its creator's profile, so this resolves
     * JWT → User → profile → TrainingProgram exactly as {@link #isCourseOwner(UUID)} does.
     * <p>
     * Two profiles have to be tried, not one. The course-creator dashboard stamps
     * {@code course_creator_uuid} with the caller's course-creator profile, but the instructor
     * dashboard's program builder stamps it with the caller's <em>instructor</em> profile, so
     * programs authored there are on record under an instructor UUID. Accepting either is what
     * keeps an instructor able to edit, publish and delete the programs they built; narrowing to
     * the course-creator profile alone would lock them out of their own work.
     * <p>
     * Memoised per request and per program because the route guard and the service behind it both
     * ask, and each answer costs a program load.
     *
     * @param programUuid UUID of the training program to check
     * @return true if the current user created the program, false otherwise
     */
    @Override
    public boolean isProgramOwner(UUID programUuid) {
        if (programUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_OWNED_PROGRAM_PREFIX + programUuid, () -> {
            try {
                UUID userUuid = currentUserUuid();
                if (userUuid == null) {
                    return false;
                }
                Set<UUID> creatorIdentities = programCreatorIdentities(userUuid);
                if (creatorIdentities.isEmpty()) {
                    return false;
                }
                return trainingProgramRepository.findByUuid(programUuid)
                        .map(program -> program.getCourseCreatorUuid() != null
                                && creatorIdentities.contains(program.getCourseCreatorUuid()))
                        .orElse(false);
            } catch (Exception e) {
                log.error("Error checking program ownership for program: {}", programUuid, e);
                return false;
            }
        });
    }

    /**
     * The profile UUIDs a training program's {@code course_creator_uuid} may legitimately hold for
     * this user - their course-creator profile and their instructor profile. See
     * {@link #isProgramOwner(UUID)} for why both count.
     * <p>
     * Only ever asked of the caller, so the instructor half comes from the caller's memoised
     * instructor identity rather than a fresh lookup per program.
     */
    private Set<UUID> programCreatorIdentities(UUID userUuid) {
        Set<UUID> identities = new HashSet<>();
        courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).ifPresent(identities::add);
        UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
        if (instructorUuid != null) {
            identities.add(instructorUuid);
        }
        return identities;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The association is resolved first and its own program decides, because
     * {@code ProgramCourseServiceImpl} writes the row named by {@code programCourseUuid} and never
     * looks at the path - so trusting the path would let a caller quote a program they own alongside
     * a stranger's row.
     */
    @Override
    public boolean canWriteProgramCourse(UUID programUuid, UUID programCourseUuid, UUID payloadProgramUuid) {
        UUID owningProgramUuid = programCourseUuid == null
                ? programUuid
                : programCourseRepository.findByUuid(programCourseUuid)
                        .map(ProgramCourse::getProgramUuid)
                        .orElse(programUuid);
        return ownsBothPrograms(owningProgramUuid, payloadProgramUuid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resolved exactly as {@link #canWriteProgramCourse(UUID, UUID, UUID)} is, and for the same
     * reason: {@code ProgramRequirementServiceImpl} writes the row named by {@code requirementUuid}
     * and ignores the path.
     */
    @Override
    public boolean canWriteProgramRequirement(UUID programUuid, UUID requirementUuid, UUID payloadProgramUuid) {
        UUID owningProgramUuid = requirementUuid == null
                ? programUuid
                : programRequirementRepository.findByUuid(requirementUuid)
                        .map(ProgramRequirement::getProgramUuid)
                        .orElse(programUuid);
        return ownsBothPrograms(owningProgramUuid, payloadProgramUuid);
    }

    /**
     * The caller must own the program the row lives in and, when the body names a program to move it
     * to, own that one too - otherwise a program's courses or requirements could be pushed into a
     * stranger's program.
     */
    private boolean ownsBothPrograms(UUID owningProgramUuid, UUID payloadProgramUuid) {
        if (!isProgramOwner(owningProgramUuid)) {
            return false;
        }
        return payloadProgramUuid == null
                || payloadProgramUuid.equals(owningProgramUuid)
                || isProgramOwner(payloadProgramUuid);
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
     * True when the caller holds a course enrolment for this course that still allows access.
     * <p>
     * Resolves the student from the authenticated principal, never from a request parameter, so
     * a learner cannot claim somebody else's enrolment. {@code course_enrollments} is unique on
     * (student, course), so the lookup is unambiguous.
     */
    @Override
    public boolean isEnrolledLearner(UUID courseUuid) {
        return courseUuid != null && enrolledCourseUuids().contains(courseUuid);
    }

    /**
     * The caller's enrolable courses, loaded once per request.
     * <p>
     * A learner holds a handful of enrolments, so fetching the whole set costs one query and then
     * answers every course check by set membership. Checking course-by-course instead would cost a
     * query per course, which a page listing material from several courses pays repeatedly.
     */
    @Override
    public Set<UUID> enrolledCourseUuids() {
        return requestScopedCache.get(CACHE_ENROLLED_COURSES, () -> {
            try {
                UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
                if (studentUuid == null) {
                    return Set.<UUID>of();
                }
                return Set.copyOf(courseEnrollmentRepository.findCourseUuidsByStudentUuidAndStatusIn(
                        studentUuid, EnrollmentStatus.ACCESS_ALLOWING));
            } catch (Exception e) {
                log.error("Error loading enrolled courses for the current caller", e);
                return Set.<UUID>of();
            }
        });
    }

    /**
     * Staff reading rights plus enrolled learners. See the SPI javadoc for why this is a sibling
     * of {@link #canReadCourseContent(UUID)} rather than a widening of it.
     */
    @Override
    public boolean canReadCourseAsLearner(UUID courseUuid) {
        return canReadCourseContent(courseUuid) || isEnrolledLearner(courseUuid);
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

            // The caller's own instructor identity, resolved once per request: a gradebook page
            // asks this for every course it renders.
            UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
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
