package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.internal.LearnerAssessmentScope;
import apps.sarafrika.elimika.course.internal.security.LearnerContentAccess;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLessonBatchServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LearnerContentAccess learnerContentAccess;
    @Mock
    private CourseSecuritySpi courseSecurityService;
    @Mock
    private LearnerAssessmentScope learnerAssessmentScope;

    @InjectMocks
    private CourseLessonBatchService service;

    @Test
    @SuppressWarnings("unchecked")
    void learnerGetsLessonsOfReadableCoursesOnlyInOneQuery() {
        UUID enrolled = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        when(learnerContentAccess.isStaff()).thenReturn(false);
        when(courseSecurityService.canReadCourseAsLearner(enrolled)).thenReturn(true);
        when(courseSecurityService.canReadCourseAsLearner(stranger)).thenReturn(false);
        when(learnerAssessmentScope.seesAllLearners()).thenReturn(false);
        Lesson lesson = new Lesson();
        lesson.setUuid(UUID.randomUUID());
        lesson.setCourseUuid(enrolled);
        when(lessonRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(lesson));

        List<LessonDTO> result = service.getLessons(List.of(enrolled, stranger, enrolled));

        assertThat(result).singleElement().satisfies(dto -> assertThat(dto.courseUuid()).isEqualTo(enrolled));
        verify(courseSecurityService, times(1)).canReadCourseAsLearner(enrolled);
        verify(lessonRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void staffSkipThePerCourseLearnerCheck() {
        UUID course = UUID.randomUUID();
        when(learnerContentAccess.isStaff()).thenReturn(true);
        when(learnerAssessmentScope.seesAllLearners()).thenReturn(true);
        when(lessonRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        assertThat(service.getLessons(List.of(course))).isEmpty();

        verify(courseSecurityService, never()).canReadCourseAsLearner(any());
    }

    @Test
    void noReadableCourseSkipsTheLessonQuery() {
        UUID stranger = UUID.randomUUID();
        when(learnerContentAccess.isStaff()).thenReturn(false);
        when(courseSecurityService.canReadCourseAsLearner(stranger)).thenReturn(false);

        assertThat(service.getLessons(List.of(stranger))).isEmpty();

        verifyNoInteractions(lessonRepository);
    }

    @Test
    void emptyAndOversizedBatchesAreRejected() {
        assertThatThrownBy(() -> service.getLessons(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getLessons(null)).isInstanceOf(IllegalArgumentException.class);
        List<UUID> tooMany = IntStream.rangeClosed(0, CourseLessonBatchService.MAX_BATCH_SIZE)
                .mapToObj(i -> UUID.randomUUID()).toList();
        assertThatThrownBy(() -> service.getLessons(tooMany)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(lessonRepository, courseSecurityService);
    }
}
