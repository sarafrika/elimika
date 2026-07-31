package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.CourseRubricAssociation;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRubricAssociationRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the rule behind the learner-readable rubric endpoints: being graded against a rubric
 * earns the right to read it, and nothing wider than that.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LearnerContentAccessTest {

    private static final UUID RUBRIC_UUID = UUID.randomUUID();
    private static final UUID ENROLLED_COURSE_UUID = UUID.randomUUID();

    @Mock private QuizRepository quizRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseRubricAssociationRepository courseRubricAssociationRepository;
    @Mock private CourseSecuritySpi courseSecurityService;
    @Mock private DomainSecurityService domainSecurityService;

    private LearnerContentAccess access;

    @BeforeEach
    void setUp() {
        access = new LearnerContentAccess(
                quizRepository, assignmentRepository, lessonRepository,
                courseRubricAssociationRepository, courseSecurityService,
                domainSecurityService, new RequestScopedCache());

        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(false);
        when(domainSecurityService.isCourseCreator()).thenReturn(false);

        // RequestScopedCache memoises against the bound request; without one it recomputes.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void staffReadAnyRubricWithoutAnEnrolmentLookup() {
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(true);

        assertThat(access.canReadRubric(RUBRIC_UUID)).isTrue();
        verify(courseRubricAssociationRepository, never()).findByRubricUuid(any());
    }

    @Test
    void aLearnerMayReadARubricAttachedToACourseTheyAreEnrolledIn() {
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(ENROLLED_COURSE_UUID));
        when(courseRubricAssociationRepository.findByRubricUuid(RUBRIC_UUID))
                .thenReturn(List.of(association(UUID.randomUUID()), association(ENROLLED_COURSE_UUID)));

        assertThat(access.canReadRubric(RUBRIC_UUID)).isTrue();
    }

    @Test
    void aLearnerMayNotReadARubricUsedOnlyByCoursesTheyAreNotIn() {
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(ENROLLED_COURSE_UUID));
        when(courseRubricAssociationRepository.findByRubricUuid(RUBRIC_UUID))
                .thenReturn(List.of(association(UUID.randomUUID())));

        assertThat(access.canReadRubric(RUBRIC_UUID)).isFalse();
    }

    @Test
    void anUnattachedRubricIsNotReadableByALearner() {
        // A rubric still being drafted belongs to nobody's course yet.
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(ENROLLED_COURSE_UUID));
        when(courseRubricAssociationRepository.findByRubricUuid(RUBRIC_UUID)).thenReturn(List.of());

        assertThat(access.canReadRubric(RUBRIC_UUID)).isFalse();
    }

    @Test
    void aCallerWithNoEnrolmentsReadsNothingAndCostsNoRubricQuery() {
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of());

        assertThat(access.canReadRubric(RUBRIC_UUID)).isFalse();
        verify(courseRubricAssociationRepository, never()).findByRubricUuid(any());
    }

    @Test
    void aMissingRubricUuidIsARefusal() {
        assertThat(access.canReadRubric(null)).isFalse();
    }

    @Test
    void aLookupFailureDeniesRatherThanGrants() {
        when(courseSecurityService.enrolledCourseUuids()).thenThrow(new IllegalStateException("boom"));

        assertThat(access.canReadRubric(RUBRIC_UUID)).isFalse();
    }

    @Test
    void theDecisionIsMemoisedForTheRequest() {
        // These predicates run per item on listing endpoints; re-deciding would cost a query each.
        when(courseSecurityService.enrolledCourseUuids()).thenReturn(Set.of(ENROLLED_COURSE_UUID));
        when(courseRubricAssociationRepository.findByRubricUuid(RUBRIC_UUID))
                .thenReturn(List.of(association(ENROLLED_COURSE_UUID)));

        assertThat(access.canReadRubric(RUBRIC_UUID)).isTrue();
        assertThat(access.canReadRubric(RUBRIC_UUID)).isTrue();

        verify(courseRubricAssociationRepository, times(1)).findByRubricUuid(RUBRIC_UUID);
    }

    private CourseRubricAssociation association(UUID courseUuid) {
        CourseRubricAssociation association = new CourseRubricAssociation();
        association.setCourseUuid(courseUuid);
        association.setRubricUuid(RUBRIC_UUID);
        return association;
    }
}
