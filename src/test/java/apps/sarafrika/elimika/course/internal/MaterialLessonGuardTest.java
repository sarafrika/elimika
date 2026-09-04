package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The lesson a body names is a destination the endpoint guard never saw, so it is authorised
 * separately.
 */
@ExtendWith(MockitoExtension.class)
class MaterialLessonGuardTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseSecuritySpi courseSecurityService;
    @Mock
    private DomainSecurityService domainSecurityService;

    private MaterialLessonGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MaterialLessonGuard(lessonRepository, courseSecurityService, domainSecurityService);
    }

    @Test
    void admitsALessonInACourseTheCallerMayTeach() {
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        when(lessonRepository.findCourseViewByUuid(lessonUuid))
                .thenReturn(Optional.of(new MaterialCourseView(courseUuid, null, null, null)));
        when(courseSecurityService.canManageCourseGradebook(courseUuid)).thenReturn(true);

        assertThatCode(() -> guard.requireManageableLesson(lessonUuid)).doesNotThrowAnyException();
    }

    @Test
    void refusesALessonInSomebodyElsesCourse() {
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        when(lessonRepository.findCourseViewByUuid(lessonUuid))
                .thenReturn(Optional.of(new MaterialCourseView(courseUuid, null, null, null)));
        when(courseSecurityService.canManageCourseGradebook(courseUuid)).thenReturn(false);

        assertThatThrownBy(() -> guard.requireManageableLesson(lessonUuid))
                .hasMessageContaining("may add material to");
    }

    @Test
    void refusesALessonThatDoesNotExist() {
        UUID lessonUuid = UUID.randomUUID();
        when(lessonRepository.findCourseViewByUuid(lessonUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireManageableLesson(lessonUuid))
                .hasMessageContaining("may add material to");
    }

    @Test
    void leavesUnparentedMaterialAloneAndNeverQueries() {
        assertThatCode(() -> guard.requireManageableLesson(null)).doesNotThrowAnyException();
    }
}
