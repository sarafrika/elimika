package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private LearnerAssessmentScope scope;

    @BeforeEach
    void setUp() {
        scope = new LearnerAssessmentScope(courseEnrollmentRepository, domainSecurityService);
    }

    @Test
    void teachingStaffAreNotNarrowedToOwnEnrollments() {
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(true);

        Specification<QuizAttempt> base = (root, query, cb) -> null;
        assertThat(scope.restrictToCaller(base, "enrollmentUuid")).isSameAs(base);
        verify(courseEnrollmentRepository, never()).findEnrollmentUuidsByStudentUuid(any());
    }

    @Test
    void courseCreatorsAreNotNarrowedToOwnEnrollments() {
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
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(false);
        when(domainSecurityService.isCourseCreator()).thenReturn(false);
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

    @Test
    void studentWithoutEnrollmentsMatchesNothingRatherThanEverything() {
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(false);
        when(domainSecurityService.isCourseCreator()).thenReturn(false);
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        Specification<QuizAttempt> restricted = scope.restrictToCaller(null, "enrollmentUuid");

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate nothing = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(nothing);

        assertThat(restricted.toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb)).isSameAs(nothing);
    }
}
