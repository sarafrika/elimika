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
 * The ladder answers "what is the highest footing this person holds", which on its own is the same
 * answer on every page. One account is routinely an administrator, an instructor and a learner at
 * once, and such a user reading their own learner dashboard was handed an administrator's course —
 * full lesson bodies on a course they had never enrolled in. {@link CourseFootingCap} supplies the
 * other half of the question: each rung is now skipped unless the dashboard the request came from
 * is allowed to stand on it. Skipping a rung only ever moves the caller further down the ladder, so
 * a footing can be narrowed by the acting dashboard and never widened by it.
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
    private final CourseFootingCap courseFootingCap;
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
     * <p>
     * Capped by the reader's dashboard all the same. Describing the school is what decides the
     * footing, but transmitting the syllabus is still a thing done to somebody, and a route that
     * takes the organisation from the path would otherwise be the way around every cap on the one
     * beside it: name any approved school and read the course from a learner's page.
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
            if (mayStandOn(CourseContentAccess.ORGANISATION)
                    && approved(courseUuid, CourseTrainingApplicantType.ORGANISATION, applicants)) {
                return CourseContentAccess.ORGANISATION;
            }
            if (mayStandOn(CourseContentAccess.PENDING)
                    && applied(courseUuid, CourseTrainingApplicantType.ORGANISATION, applicants)) {
                return CourseContentAccess.PENDING;
            }
            return CourseContentAccess.PROSPECT;
        } catch (Exception e) {
            log.error("Error resolving content access for organisation {} on course {}", organisationUuid, courseUuid, e);
            return CourseContentAccess.PROSPECT;
        }
    }

    /**
     * The ladder, with every rung guarded by the dashboard the request came from.
     * <p>
     * Each test is {@code mayStandOn(rung) && <the real entitlement check>}, and the order of the
     * conjunction is the cheap half first: a dashboard that cannot reach a rung never pays for the
     * query that would have decided it. A capped rung falls through to the next one rather than
     * failing the whole resolution, which is what makes a student-acting administrator who is
     * genuinely enrolled land on {@code STUDENT} instead of on nothing.
     */
    private CourseContentAccess resolve(UUID courseUuid) {
        try {
            if (mayStandOn(CourseContentAccess.CREATOR) && courseSecurityService.isCourseOwner(courseUuid)) {
                return CourseContentAccess.CREATOR;
            }
            if (mayStandOn(CourseContentAccess.ADMIN) && domainSecurityService.isPlatformAdmin()) {
                return CourseContentAccess.ADMIN;
            }

            UUID userUuid = domainSecurityService.getCurrentUserUuid();
            if (userUuid == null) {
                // Anonymous, or a token for a user this deployment has never seen.
                return CourseContentAccess.PROSPECT;
            }

            boolean readsAsOrganisation = mayStandOn(CourseContentAccess.ORGANISATION);
            List<UUID> organisations = readsAsOrganisation ? teachingOrganisations.of(userUuid) : List.of();
            if (approved(courseUuid, CourseTrainingApplicantType.ORGANISATION, organisations)) {
                return CourseContentAccess.ORGANISATION;
            }

            boolean readsAsInstructor = mayStandOn(CourseContentAccess.INSTRUCTOR);
            UUID instructorUuid = readsAsInstructor ? domainSecurityService.getCurrentInstructorUuid() : null;
            List<UUID> instructors = instructorUuid == null ? List.of() : List.of(instructorUuid);
            if (approved(courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructors)) {
                return CourseContentAccess.INSTRUCTOR;
            }

            if (mayStandOn(CourseContentAccess.STUDENT) && courseSecurityService.isEnrolledLearner(courseUuid)) {
                return CourseContentAccess.STUDENT;
            }

            // Every approved footing has already returned, so an application that exists here is by
            // definition one that has not been approved. The applicant lists are the capped ones, so
            // an instructor's dashboard never reports pending on the strength of a school's
            // application, nor a school's on the strength of one of its instructors'.
            if (mayStandOn(CourseContentAccess.PENDING)
                    && (applied(courseUuid, CourseTrainingApplicantType.ORGANISATION, organisations)
                            || applied(courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructors))) {
                return CourseContentAccess.PENDING;
            }

            if (mayStandOn(CourseContentAccess.APPLICANT)
                    && domainSecurityService.hasAnyDomain(TEACHING_DOMAINS)) {
                return CourseContentAccess.APPLICANT;
            }

            return CourseContentAccess.PROSPECT;
        } catch (Exception e) {
            log.error("Error resolving content access for course {}", courseUuid, e);
            return CourseContentAccess.PROSPECT;
        }
    }

    /** Whether the dashboard this request came from is allowed to stand on {@code footing}. */
    private boolean mayStandOn(CourseContentAccess footing) {
        return courseFootingCap.permits(footing);
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
