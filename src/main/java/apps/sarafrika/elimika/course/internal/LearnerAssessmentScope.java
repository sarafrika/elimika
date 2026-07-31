package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Decides how much of an assessment listing the current caller may see.
 * <p>
 * Quiz attempts and assignment submissions are keyed by course enrollment, not by student,
 * so a listing endpoint that filters only by quiz/assignment returns every learner's work.
 * Teaching staff need exactly that; a student must only ever see rows belonging to their own
 * enrollments. This component centralises the rule so both assessment listings apply it
 * identically, and so students can be granted access to those endpoints without the listing
 * leaking their classmates' scores.
 */
@Component
@RequiredArgsConstructor
public class LearnerAssessmentScope {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final DomainSecurityService domainSecurityService;

    /**
     * Whether the caller may see every learner's work. Mirrors the management domains the
     * assessment endpoints were originally restricted to, so staff listings are unchanged.
     */
    public boolean seesAllLearners() {
        return domainSecurityService.isInstructorOrAdmin() || domainSecurityService.isCourseCreator();
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
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        if (studentUuid == null) {
            return Set.of();
        }
        return Set.copyOf(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid));
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
}
