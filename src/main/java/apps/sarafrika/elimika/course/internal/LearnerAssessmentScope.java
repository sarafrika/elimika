package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Owns the relationship between the current caller and the course enrolments they may act through.
 * <p>
 * Two problems live here. First, quiz attempts and assignment submissions are keyed by course
 * enrolment rather than by student, so a listing filtered only by quiz or assignment returns every
 * learner's work — teaching staff need exactly that, a learner must never get it. Second, callers
 * cannot reliably name their own enrolment: a learner has one {@code course_enrollments} row per
 * course but a {@code class_enrollments} row per scheduled session, and clients routinely send the
 * latter where the former is required. Both problems are answered from the authenticated principal,
 * never from a request parameter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearnerAssessmentScope {

    private static final String CACHE_ENROLLMENT_UUIDS = "learnerScope.enrollmentUuids";
    private static final String CACHE_SEES_ALL = "learnerScope.seesAllLearners";

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final DomainSecurityService domainSecurityService;
    private final EnrollmentLookupService enrollmentLookupService;
    private final CourseSecuritySpi courseSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * Whether the caller may see every learner's work. Mirrors the management domains the
     * assessment endpoints were originally restricted to, so staff listings are unchanged.
     */
    public boolean seesAllLearners() {
        return requestScopedCache.get(CACHE_SEES_ALL, () ->
                domainSecurityService.isInstructorOrAdmin() || domainSecurityService.isCourseCreator());
    }

    /**
     * Course enrollment UUIDs owned by the calling student. Empty when the caller has no
     * student profile, which correctly resolves to "sees nothing" rather than "sees everything".
     * <p>
     * Deliberately ignores {@code EnrollmentStatus.allowsAccess()}: this answers "which rows are
     * mine", not "which courses may I still enter". A learner who dropped a course keeps the right
     * to see the work they submitted while enrolled.
     */
    public Set<UUID> callerEnrollmentUuids() {
        return requestScopedCache.get(CACHE_ENROLLMENT_UUIDS, () -> {
            UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
            if (studentUuid == null) {
                return Set.<UUID>of();
            }
            return Set.copyOf(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid));
        });
    }

    /**
     * Narrows a query to the caller's own enrollments unless they are teaching staff.
     *
     * @param base                the query so far, may be {@code null} for "no filter yet"
     * @param enrollmentAttribute name of the entity attribute holding the enrollment UUID
     */
    public <T> Specification<T> restrictToCaller(Specification<T> base, String enrollmentAttribute) {
        if (seesAllLearners()) {
            return base;
        }

        Set<UUID> ownEnrollments = callerEnrollmentUuids();
        Specification<T> own = ownEnrollments.isEmpty()
                ? (root, query, cb) -> cb.disjunction()
                : (root, query, cb) -> root.get(enrollmentAttribute).in(ownEnrollments);

        return base == null ? own : base.and(own);
    }

    /**
     * Whether the given course enrolment belongs to the calling learner. Proves ownership only —
     * callers that also care which course the enrolment sits under must check that separately.
     */
    public boolean ownsEnrollment(UUID courseEnrollmentUuid) {
        return courseEnrollmentUuid != null && callerEnrollmentUuids().contains(courseEnrollmentUuid);
    }

    /**
     * Courses the calling learner may currently enter. Unlike {@link #callerEnrollmentUuids()} this
     * respects {@code allowsAccess()}, because it gates reading course material rather than reading
     * one's own past work.
     */
    public Set<UUID> callerEnrolledCourseUuids() {
        return courseSecurityService.enrolledCourseUuids();
    }

    /**
     * Resolves the course enrolment an assessment call should act through.
     * <p>
     * Clients cannot be trusted to name the right enrolment — a learner holds one course enrolment
     * per course but a class enrolment per session, and the two UUID spaces are easy to confuse. So:
     * a learner never needs to supply one, and a supplied UUID from the wrong space is translated
     * rather than rejected. Staff, who have no enrolment of their own, must always be explicit.
     *
     * @param courseUuid the course the assessment belongs to
     * @param supplied   enrolment UUID from the request, may be {@code null}
     * @throws IllegalArgumentException  when a staff caller omits the enrolment
     * @throws ResourceNotFoundException when no enrolment can be resolved
     * @throws AccessDeniedException     when the enrolment is not the caller's, is for another
     *                                   course, or no longer allows access
     */
    public CourseEnrollment resolveEnrollment(UUID courseUuid, UUID supplied) {
        CourseEnrollment enrollment = supplied == null
                ? deriveForCaller(courseUuid)
                : lookupSupplied(courseUuid, supplied);

        if (!courseUuid.equals(enrollment.getCourseUuid())) {
            throw new AccessDeniedException("Course enrollment does not belong to this course.");
        }
        if (enrollment.getStatus() == null || !enrollment.getStatus().allowsAccess()) {
            throw new AccessDeniedException("Course enrollment does not allow access to this course.");
        }
        if (domainSecurityService.isStudent()
                && !domainSecurityService.isStudentWithUuid(enrollment.getStudentUuid())) {
            throw new AccessDeniedException("Students may only act through their own course enrollment.");
        }
        return enrollment;
    }

    /**
     * No enrolment supplied: derive it for a learner, refuse to guess for staff. A staff caller has
     * no enrolment of their own, so guessing would silently act on the wrong learner's record.
     */
    private CourseEnrollment deriveForCaller(UUID courseUuid) {
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        if (studentUuid == null) {
            throw new IllegalArgumentException(
                    "enrollment_uuid is required when acting on behalf of a learner.");
        }
        return courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not enrolled in this course."));
    }

    /**
     * Enrolment supplied: use it if it names a course enrolment, otherwise try to translate it from
     * the class-enrolment UUID space before giving up.
     */
    private CourseEnrollment lookupSupplied(UUID courseUuid, UUID supplied) {
        return courseEnrollmentRepository.findByUuid(supplied)
                .orElseGet(() -> translateClassEnrollment(courseUuid, supplied));
    }

    /**
     * Compatibility shim for clients still sending a {@code class_enrollments} UUID where a
     * {@code course_enrollments} UUID is required. Resolves the owning student and looks up their
     * course enrolment instead.
     * <p>
     * Deliberately noisy: the warning is the signal for when this shim can be deleted. Remove it
     * once no caller trips it.
     */
    private CourseEnrollment translateClassEnrollment(UUID courseUuid, UUID supplied) {
        UUID studentUuid = enrollmentLookupService.getEnrollmentStudentUuid(supplied).orElse(null);
        if (studentUuid == null) {
            throw new ResourceNotFoundException(
                    String.format("Course enrollment with ID %s not found", supplied));
        }

        CourseEnrollment translated = courseEnrollmentRepository
                .findByStudentUuidAndCourseUuid(studentUuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Course enrollment with ID %s not found", supplied)));

        log.warn("Legacy class enrollment UUID {} translated to course enrollment {} for course {}. "
                + "Callers should send the course enrollment UUID or omit it entirely.",
                supplied, translated.getUuid(), courseUuid);
        return translated;
    }
}
