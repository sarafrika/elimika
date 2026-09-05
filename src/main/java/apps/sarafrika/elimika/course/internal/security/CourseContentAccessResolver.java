package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Answers "on what footing is this caller reading this course?" — once, server-side.
 * <p>
 * Every other approach the platform has tried put this decision in the browser: the client looked
 * at its own domain, or at whether {@code course_creator_uuid} matched, and chose a page. That is
 * not a decision a client can make. Only the server knows whether a training application was
 * approved, whether an enrolment still allows access, or whether an approval was revoked last
 * night — and only the server can then decline to transmit the lesson bodies. So the answer travels
 * with the payload, and the client renders the state it is handed.
 * <p>
 * Resolution is first-match-wins down a ladder of decreasing entitlement. Order carries meaning:
 * a course creator who also happens to be enrolled reads as {@code creator}, and a learner who has
 * also applied to teach reads as {@code student}, because the strongest footing is the one whose
 * page they want. Anything that throws resolves to {@link CourseContentAccess#PROSPECT}, so a
 * lookup failure narrows what is sent rather than widening it.
 * <p>
 * Memoised per request: a request that asks twice — the endpoint and, later, an assembler — costs
 * one resolution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseContentAccessResolver {

    private static final String CACHE_PREFIX = "courseContentAccess.";

    /** The domains that make a caller somebody who could apply to train, rather than a browser. */
    private static final UserDomain[] TEACHING_DOMAINS = {UserDomain.instructor, UserDomain.course_creator};

    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final TeachingOrganisations teachingOrganisations;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final RequestScopedCache requestScopedCache;

    /**
     * @param courseUuid the course being read
     * @return the caller's footing, never null
     */
    public CourseContentAccess resolveForCaller(UUID courseUuid) {
        if (courseUuid == null) {
            return CourseContentAccess.PROSPECT;
        }
        return requestScopedCache.get(CACHE_PREFIX + courseUuid, () -> resolve(courseUuid));
    }

    /**
     * The footing a named organisation is on, independent of who is asking on its behalf.
     * <p>
     * Backs the deprecated organisation-scoped route, where the caller may be an admin reading for
     * a school they are not a member of: the answer has to describe the school, not the reader.
     *
     * @param courseUuid       the course being read
     * @param organisationUuid the organisation the content is scoped to
     * @return {@code organisation} when approved, {@code pending} when it has applied and is not,
     * {@code prospect} otherwise
     */
    public CourseContentAccess resolveForOrganisation(UUID courseUuid, UUID organisationUuid) {
        if (courseUuid == null || organisationUuid == null) {
            return CourseContentAccess.PROSPECT;
        }
        try {
            List<UUID> applicants = List.of(organisationUuid);
            if (approved(courseUuid, CourseTrainingApplicantType.ORGANISATION, applicants)) {
                return CourseContentAccess.ORGANISATION;
            }
            if (applied(courseUuid, CourseTrainingApplicantType.ORGANISATION, applicants)) {
                return CourseContentAccess.PENDING;
            }
            return CourseContentAccess.PROSPECT;
        } catch (Exception e) {
            log.error("Error resolving content access for organisation {} on course {}", organisationUuid, courseUuid, e);
            return CourseContentAccess.PROSPECT;
        }
    }

    private CourseContentAccess resolve(UUID courseUuid) {
        try {
            if (courseSecurityService.isCourseOwner(courseUuid)) {
                return CourseContentAccess.CREATOR;
            }
            if (domainSecurityService.isPlatformAdmin()) {
                return CourseContentAccess.ADMIN;
            }

            UUID userUuid = domainSecurityService.getCurrentUserUuid();
            if (userUuid == null) {
                // Anonymous, or a token for a user this deployment has never seen.
                return CourseContentAccess.PROSPECT;
            }

            List<UUID> organisations = teachingOrganisations.of(userUuid);
            if (approved(courseUuid, CourseTrainingApplicantType.ORGANISATION, organisations)) {
                return CourseContentAccess.ORGANISATION;
            }

            UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
            List<UUID> instructors = instructorUuid == null ? List.of() : List.of(instructorUuid);
            if (approved(courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructors)) {
                return CourseContentAccess.INSTRUCTOR;
            }

            if (courseSecurityService.isEnrolledLearner(courseUuid)) {
                return CourseContentAccess.STUDENT;
            }

            // Every approved footing has already returned, so an application that exists here is by
            // definition one that has not been approved.
            if (applied(courseUuid, CourseTrainingApplicantType.ORGANISATION, organisations)
                    || applied(courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructors)) {
                return CourseContentAccess.PENDING;
            }

            if (domainSecurityService.hasAnyDomain(TEACHING_DOMAINS)) {
                return CourseContentAccess.APPLICANT;
            }

            return CourseContentAccess.PROSPECT;
        } catch (Exception e) {
            log.error("Error resolving content access for course {}", courseUuid, e);
            return CourseContentAccess.PROSPECT;
        }
    }

    private boolean approved(UUID courseUuid, CourseTrainingApplicantType type, Collection<UUID> applicants) {
        return !applicants.isEmpty()
                && courseTrainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidInAndStatus(
                        courseUuid, type, applicants, CourseTrainingApplicationStatus.APPROVED);
    }

    private boolean applied(UUID courseUuid, CourseTrainingApplicantType type, Collection<UUID> applicants) {
        return !applicants.isEmpty()
                && courseTrainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidIn(
                        courseUuid, type, applicants);
    }
}
