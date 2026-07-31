package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
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

/**
 * The assessment search endpoints translate a free-form parameter map into a query over every row in
 * the table. Opening them to learners is only safe because of the floor this class enforces.
 */
@ExtendWith(MockitoExtension.class)
class LearnerMaterialScopeTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseSecuritySpi courseSecurityService;
    @Mock
    private LearnerAssessmentScope learnerAssessmentScope;

    private LearnerMaterialScope scope;

    @BeforeEach
    void setUp() {
        scope = new LearnerMaterialScope(lessonRepository, courseSecurityService,
                learnerAssessmentScope, new RequestScopedCache());
    }

    @Test
    void staffSearchesArePassedThroughUntouched() {
        when(learnerAssessmentScope.seesAllLearners()).thenReturn(true);

        Specification<Quiz> base = (root, query, cb) -> null;
        assertThat(scope.restrictQuizzes(base)).isSameAs(base);
        verify(lessonRepository, never()).findVisibleLessonUuidsByCourseUuidIn(any(), any());
    }

    @Test
    void visibleLessonsAreTheseOfTheCallersEnrolledCourses() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(courseUuid));
        when(lessonRepository.findVisibleLessonUuidsByCourseUuidIn(Set.of(courseUuid), ContentStatus.PUBLISHED))
                .thenReturn(List.of(lessonUuid));

        assertThat(scope.visibleLessonUuids()).containsExactly(lessonUuid);
    }

    @Test
    void aLearnerEnrolledInNothingQueriesNoLessons() {
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of());

        assertThat(scope.visibleLessonUuids()).isEmpty();
        verify(lessonRepository, never()).findVisibleLessonUuidsByCourseUuidIn(any(), any());
    }

    @Test
    void aLearnerWithNoVisibleLessonsMatchesNothingRatherThanEverything() {
        // Fail closed: an unscoped search would hand over the platform's whole question bank.
        when(learnerAssessmentScope.seesAllLearners()).thenReturn(false);
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of());

        Specification<Quiz> restricted = scope.restrictQuizzes(null);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate nothing = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(nothing);

        assertThat(restricted.toPredicate(mock(Root.class), mock(CriteriaQuery.class), cb)).isSameAs(nothing);
    }

    @Test
    void aLearnerQuizSearchIsConfinedToPublishedQuizzesOnVisibleLessons() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        when(learnerAssessmentScope.seesAllLearners()).thenReturn(false);
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(courseUuid));
        when(lessonRepository.findVisibleLessonUuidsByCourseUuidIn(Set.of(courseUuid), ContentStatus.PUBLISHED))
                .thenReturn(List.of(lessonUuid));

        Specification<Quiz> restricted = scope.restrictQuizzes(null);

        Root<Quiz> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Object> activePath = mock(Path.class);
        Path<Object> lessonPath = mock(Path.class);
        Predicate publishedPredicate = mock(Predicate.class);
        Predicate activePredicate = mock(Predicate.class);
        Predicate lessonPredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(root.get("active")).thenReturn(activePath);
        when(root.get("lessonUuid")).thenReturn(lessonPath);
        when(cb.equal(statusPath, ContentStatus.PUBLISHED)).thenReturn(publishedPredicate);
        when(cb.isTrue(any())).thenReturn(activePredicate);
        when(cb.and(publishedPredicate, activePredicate)).thenReturn(combined);
        when(lessonPath.in(Set.of(lessonUuid))).thenReturn(lessonPredicate);
        when(cb.and(combined, lessonPredicate)).thenReturn(finalPredicate);

        assertThat(restricted.toPredicate(root, mock(CriteriaQuery.class), cb)).isSameAs(finalPredicate);
    }
}
