package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
     * Whether the caller is teaching staff rather than a learner, and so may be shown unpublished
     * material in a listing.
     * <p>
     * A platform-wide domain test, and therefore <em>not</em> fit to decide whose work a caller may
     * read — {@link #restrictToCaller(Specification, String)} answers that per course instead.
     * Kept for the material listings that only need to know "draft or published".
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
     * Narrows a query of learner work to the rows the caller is entitled to see: their own, plus —
     * for teaching staff — every learner on a course they actually mark.
     * <p>
     * Holding a teaching domain is not the question. The domain is platform-wide, so answering it
     * would hand every instructor and course creator every learner's attempts and submissions for
     * any quiz or assignment they cared to name; the course-scoped set is what "my learners" means.
     * Only a platform admin sees the query unnarrowed.
     * <p>
     * Staff are matched through a subquery rather than by expanding their courses into enrolment
     * UUIDs, because a popular course has thousands of enrolments and an {@code IN} list of that
     * size is not a query plan anybody wants.
     *
     * @param base                the query so far, may be {@code null} for "no filter yet"
     * @param enrollmentAttribute name of the entity attribute holding the enrollment UUID
     */
    public <T> Specification<T> restrictToCaller(Specification<T> base, String enrollmentAttribute) {
        if (domainSecurityService.isPlatformAdmin()) {
            return base;
        }

        Set<UUID> ownEnrollments = callerEnrollmentUuids();
        Set<UUID> markedCourses = courseSecurityService.manageableCourseUuids();

        Specification<T> visible = (root, query, cb) -> {
            List<Predicate> alternatives = new ArrayList<>();
            if (!ownEnrollments.isEmpty()) {
                alternatives.add(root.get(enrollmentAttribute).in(ownEnrollments));
            }
            if (!markedCourses.isEmpty()) {
                Subquery<UUID> enrolmentsOnMyCourses = query.subquery(UUID.class);
                Root<CourseEnrollment> enrolment = enrolmentsOnMyCourses.from(CourseEnrollment.class);
                enrolmentsOnMyCourses.select(enrolment.get("uuid"))
                        .where(enrolment.get("courseUuid").in(markedCourses));
                alternatives.add(root.get(enrollmentAttribute).in(enrolmentsOnMyCourses));
            }
            return switch (alternatives.size()) {
                case 0 -> cb.disjunction();
                case 1 -> alternatives.get(0);
                default -> cb.or(alternatives.toArray(new Predicate[0]));
            };
        };

        return base == null ? visible : base.and(visible);
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
     * @throws AccessDeniedException     when the enrolment is neither the caller's own nor on a
     *                                   course they mark, is for another course, or no longer
     *                                   allows access
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
        requireMayActThrough(courseUuid, enrollment);
        return enrollment;
    }

    /**
     * Two kinds of caller may act through an enrolment: the learner it belongs to, and the staff
     * who mark that course.
     * <p>
     * The earlier form of this check exempted anyone who was not a student, which read as "staff
     * are trusted" but meant "everybody else is": any instructor on the platform could name any
     * enrolment on any course and be handed that learner's answers and the quiz's answer key. Being
     * staff <em>somewhere</em> is not the licence; marking <em>this</em> course is.
     */
    private void requireMayActThrough(UUID courseUuid, CourseEnrollment enrollment) {
        UUID callerStudentUuid = domainSecurityService.getCurrentStudentUuid();
        if (callerStudentUuid != null && callerStudentUuid.equals(enrollment.getStudentUuid())) {
            return;
        }
        if (courseSecurityService.canManageCourseGradebook(courseUuid)
                || domainSecurityService.isPlatformAdmin()) {
            return;
        }
        throw new AccessDeniedException(
                "Only the learner who owns this course enrollment, or staff who mark its course, may act through it.");
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
