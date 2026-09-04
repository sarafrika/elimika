package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnerAssessmentScopeTest {

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private DomainSecurityService domainSecurityService;
    @Mock
    private EnrollmentLookupService enrollmentLookupService;
    @Mock
    private CourseSecuritySpi courseSecurityService;

    private LearnerAssessmentScope scope;

    @BeforeEach
    void setUp() {
        scope = new LearnerAssessmentScope(courseEnrollmentRepository, domainSecurityService,
                enrollmentLookupService, courseSecurityService, new RequestScopedCache());
    }

    @Test
    void onlyPlatformAdminsSeeAssessmentRowsUnnarrowed() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        Specification<QuizAttempt> base = (root, query, cb) -> null;
        assertThat(scope.restrictToCaller(base, "enrollmentUuid")).isSameAs(base);
        verify(courseEnrollmentRepository, never()).findEnrollmentUuidsByStudentUuid(any());
    }

    @Test
    void holdingATeachingDomainDoesNotWidenTheRowsAStaffCallerSees() {
        // The domain is platform-wide. Answering it here handed every instructor every learner's
        // attempts for any quiz they cared to name; the course-scoped set is what "mine" means.
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(courseSecurityService.manageableCourseUuids()).thenReturn(Set.of());
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        Specification<QuizAttempt> base = (root, query, cb) -> null;
        Specification<QuizAttempt> restricted = scope.restrictToCaller(base, "enrollmentUuid");
        assertThat(restricted).isNotSameAs(base);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate nothing = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(nothing);

        assertThat(restricted.toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb)).isSameAs(nothing);
    }

    @Test
    void staffSeeLearnersOnTheCoursesTheyMark() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(courseSecurityService.manageableCourseUuids()).thenReturn(Set.of(UUID.randomUUID()));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        Specification<QuizAttempt> restricted = scope.restrictToCaller(null, "enrollmentUuid");

        CriteriaQuery<?> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        assertThat(restricted.toPredicate(mock(Root.class, RETURNS_DEEP_STUBS), query, cb)).isNotNull();

        // Matched through the enrolments on those courses, never by matching nothing.
        verify(query).subquery(UUID.class);
        verify(cb, never()).disjunction();
    }

    @Test
    void courseCreatorsCountAsStaffForDraftVisibility() {
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(false);
        when(domainSecurityService.isCourseCreator()).thenReturn(true);

        assertThat(scope.seesAllLearners()).isTrue();
    }

    @Test
    void studentEnrollmentsResolveFromTheAuthenticatedUserNotTheRequest() {
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid))
                .thenReturn(List.of(enrollmentUuid));

        assertThat(scope.callerEnrollmentUuids()).containsExactly(enrollmentUuid);
    }

    @Test
    void callerWithoutStudentProfileOwnsNoEnrollments() {
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        assertThat(scope.callerEnrollmentUuids()).isEmpty();
        verify(courseEnrollmentRepository, never()).findEnrollmentUuidsByStudentUuid(any());
    }

    @Test
    void studentSpecificationMatchesOnlyOwnEnrollments() {
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(courseSecurityService.manageableCourseUuids()).thenReturn(Set.of());
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid))
                .thenReturn(List.of(enrollmentUuid));

        Specification<QuizAttempt> restricted = scope.restrictToCaller(null, "enrollmentUuid");

        Root<QuizAttempt> root = mock(Root.class);
        Path<Object> enrollmentPath = mock(Path.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate inPredicate = mock(Predicate.class);
        when(root.get("enrollmentUuid")).thenReturn(enrollmentPath);
        when(enrollmentPath.in(Set.of(enrollmentUuid))).thenReturn(inPredicate);

        assertThat(restricted.toPredicate(root, mock(CriteriaQuery.class), cb)).isSameAs(inPredicate);
    }

    // ===== ENROLLMENT RESOLUTION =====
    // Clients cannot reliably name their own enrolment: a learner has one course enrolment per
    // course but a class enrolment per session, and the two UUID spaces look identical.

    @Test
    void aLearnerNeedNotSupplyAnEnrolmentAtAll() {
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        CourseEnrollment enrollment = enrollment(UUID.randomUUID(), studentUuid, courseUuid, EnrollmentStatus.ACTIVE);
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid))
                .thenReturn(Optional.of(enrollment));

        assertThat(scope.resolveEnrollment(courseUuid, null)).isSameAs(enrollment);
    }

    @Test
    void aStaffCallerOmittingTheEnrolmentIsRejectedRatherThanGuessedFor() {
        // Staff have no enrolment of their own; guessing would act on some learner's record.
        UUID courseUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enrollment_uuid is required");
    }

    @Test
    void aClassEnrolmentUuidIsTranslatedRatherThanRejected() {
        // This is the live bug: the UI sends a class_enrollments uuid and every call 404s.
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID classEnrollmentUuid = UUID.randomUUID();
        CourseEnrollment enrollment = enrollment(UUID.randomUUID(), studentUuid, courseUuid, EnrollmentStatus.ACTIVE);

        when(courseEnrollmentRepository.findByUuid(classEnrollmentUuid)).thenReturn(Optional.empty());
        when(enrollmentLookupService.getEnrollmentStudentUuid(classEnrollmentUuid))
                .thenReturn(Optional.of(studentUuid));
        when(courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid))
                .thenReturn(Optional.of(enrollment));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);

        assertThat(scope.resolveEnrollment(courseUuid, classEnrollmentUuid)).isSameAs(enrollment);
    }

    @Test
    void aUuidInNeitherEnrolmentSpaceIsNotFound() {
        UUID courseUuid = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(courseEnrollmentRepository.findByUuid(unknown)).thenReturn(Optional.empty());
        when(enrollmentLookupService.getEnrollmentStudentUuid(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, unknown))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aLearnerNotEnrolledInTheCourseIsTold() {
        UUID courseUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not enrolled");
    }

    @Test
    void anEnrolmentForAnotherCourseIsRefused() {
        UUID courseUuid = UUID.randomUUID();
        UUID suppliedUuid = UUID.randomUUID();
        CourseEnrollment otherCourse =
                enrollment(suppliedUuid, UUID.randomUUID(), UUID.randomUUID(), EnrollmentStatus.ACTIVE);
        when(courseEnrollmentRepository.findByUuid(suppliedUuid)).thenReturn(Optional.of(otherCourse));

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, suppliedUuid))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong to this course");
    }

    @Test
    void aDroppedEnrolmentMayNotBeActedThrough() {
        UUID courseUuid = UUID.randomUUID();
        UUID suppliedUuid = UUID.randomUUID();
        CourseEnrollment dropped =
                enrollment(suppliedUuid, UUID.randomUUID(), courseUuid, EnrollmentStatus.DROPPED);
        when(courseEnrollmentRepository.findByUuid(suppliedUuid)).thenReturn(Optional.of(dropped));

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, suppliedUuid))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not allow access");
    }

    @Test
    void aStudentMayNotActThroughAnotherLearnersEnrolment() {
        UUID courseUuid = UUID.randomUUID();
        UUID suppliedUuid = UUID.randomUUID();
        UUID otherStudent = UUID.randomUUID();
        CourseEnrollment theirs = enrollment(suppliedUuid, otherStudent, courseUuid, EnrollmentStatus.ACTIVE);
        when(courseEnrollmentRepository.findByUuid(suppliedUuid)).thenReturn(Optional.of(theirs));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, suppliedUuid))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the learner who owns this course enrollment");
    }

    @Test
    void anInstructorWhoDoesNotMarkTheCourseMayNotActThroughItsEnrolments() {
        // Being staff somewhere is not the licence: the review flow hands back the answer key and
        // a named learner's answers, so it has to be staff on *this* course.
        UUID courseUuid = UUID.randomUUID();
        UUID suppliedUuid = UUID.randomUUID();
        CourseEnrollment theirs =
                enrollment(suppliedUuid, UUID.randomUUID(), courseUuid, EnrollmentStatus.ACTIVE);
        when(courseEnrollmentRepository.findByUuid(suppliedUuid)).thenReturn(Optional.of(theirs));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);
        when(courseSecurityService.canManageCourseGradebook(courseUuid)).thenReturn(false);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);

        assertThatThrownBy(() -> scope.resolveEnrollment(courseUuid, suppliedUuid))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffWhoMarkTheCourseMayActThroughALearnersEnrolment() {
        UUID courseUuid = UUID.randomUUID();
        UUID suppliedUuid = UUID.randomUUID();
        CourseEnrollment theirs =
                enrollment(suppliedUuid, UUID.randomUUID(), courseUuid, EnrollmentStatus.ACTIVE);
        when(courseEnrollmentRepository.findByUuid(suppliedUuid)).thenReturn(Optional.of(theirs));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);
        when(courseSecurityService.canManageCourseGradebook(courseUuid)).thenReturn(true);

        assertThat(scope.resolveEnrollment(courseUuid, suppliedUuid)).isSameAs(theirs);
    }

    // ===== ENROLMENT OWNERSHIP =====
    // Backs the @PreAuthorize on the read-only gradebook endpoints: a learner may look at their
    // own marks. Ownership is answered from the principal, never from the request.

    @Test
    void aLearnerOwnsTheirOwnEnrolment() {
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid))
                .thenReturn(List.of(enrollmentUuid));

        assertThat(scope.ownsEnrollment(enrollmentUuid)).isTrue();
    }

    @Test
    void aLearnerDoesNotOwnAnotherLearnersEnrolment() {
        UUID studentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findEnrollmentUuidsByStudentUuid(studentUuid))
                .thenReturn(List.of(UUID.randomUUID()));

        assertThat(scope.ownsEnrollment(UUID.randomUUID())).isFalse();
    }

    @Test
    void aCallerWithNoStudentProfileOwnsNoEnrolment() {
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        assertThat(scope.ownsEnrollment(UUID.randomUUID())).isFalse();
    }

    @Test
    void aMissingEnrolmentUuidIsNotOwned() {
        assertThat(scope.ownsEnrollment(null)).isFalse();
        verify(domainSecurityService, never()).getCurrentStudentUuid();
    }

    private CourseEnrollment enrollment(UUID uuid, UUID studentUuid, UUID courseUuid, EnrollmentStatus status) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUuid(uuid);
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(status);
        return enrollment;
    }

    @Test
    void studentWithoutEnrollmentsMatchesNothingRatherThanEverything() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(courseSecurityService.manageableCourseUuids()).thenReturn(Set.of());
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        Specification<QuizAttempt> restricted = scope.restrictToCaller(null, "enrollmentUuid");

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate nothing = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(nothing);

        assertThat(restricted.toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb)).isSameAs(nothing);
    }
}
