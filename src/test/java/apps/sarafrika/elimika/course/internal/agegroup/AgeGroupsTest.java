package apps.sarafrika.elimika.course.internal.agegroup;

import apps.sarafrika.elimika.course.dto.AgeGroupRequest;
import apps.sarafrika.elimika.course.dto.LessonHoursRequest;
import apps.sarafrika.elimika.course.internal.agegroup.AgeGroups.AgeGroupScope;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.AgeGroupRepository;
import apps.sarafrika.elimika.course.repository.AgeGroupLessonHoursRepository;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgeGroupsTest {

    @Mock private AgeGroupRepository groupRepository;
    @Mock private AgeGroupLessonHoursRepository hoursRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ProgramCourseRepository programCourseRepository;

    private AgeGroups ageGroups;

    private final UUID lessonOne = UUID.randomUUID();
    private final UUID lessonTwo = UUID.randomUUID();
    private AgeGroupScope scope;

    @BeforeEach
    void setUp() {
        ageGroups = new AgeGroups(groupRepository, hoursRepository, courseRepository,
                lessonRepository, programCourseRepository);
        scope = new AgeGroupScope(3, 12, List.of(lessonOne, lessonTwo), "this course");
    }

    @Test
    @DisplayName("non-overlapping groups inside the range with every lesson priced are accepted")
    void validGroupsPass() {
        assertThatCode(() -> ageGroups.validate(scope,
                List.of(group("Juniors", 3, 5), group("Seniors", 9, 12)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("overlapping groups are refused; gaps are fine")
    void overlapsAreRefused() {
        assertThatThrownBy(() -> ageGroups.validate(scope,
                List.of(group("Seniors", 5, 12), group("Juniors", 3, 5))))
                .hasMessageContaining("overlap");
    }

    @Test
    @DisplayName("a group outside the course's age range is refused")
    void outOfRangeIsRefused() {
        assertThatThrownBy(() -> ageGroups.validate(scope,
                List.of(group("Seniors", 10, 14))))
                .hasMessageContaining("must stay within the ages of this course");
    }

    @Test
    @DisplayName("names are unique ignoring case")
    void duplicateNamesAreRefused() {
        assertThatThrownBy(() -> ageGroups.validate(scope,
                List.of(group("Juniors", 3, 5), group("juniors ", 7, 9))))
                .hasMessageContaining("used more than once");
    }

    @Test
    @DisplayName("every active lesson needs hours, and unknown lessons are refused")
    void lessonPlanMustMatchTheCourse() {
        AgeGroupRequest missing = new AgeGroupRequest("Juniors", 3, 5,
                List.of(new LessonHoursRequest(lessonOne, new BigDecimal("0.5"))));
        assertThatThrownBy(() -> ageGroups.validate(scope, List.of(missing)))
                .hasMessageContaining("needs hours for every lesson");

        AgeGroupRequest unknown = new AgeGroupRequest("Juniors", 3, 5, List.of(
                new LessonHoursRequest(lessonOne, BigDecimal.ONE), new LessonHoursRequest(lessonTwo, BigDecimal.ONE),
                new LessonHoursRequest(UUID.randomUUID(), BigDecimal.ONE)));
        assertThatThrownBy(() -> ageGroups.validate(scope, List.of(unknown)))
                .hasMessageContaining("is not an active lesson");
    }

    @Test
    @DisplayName("a program's age range is where its courses' ranges overlap, and lessons span every course")
    void programScopeIntersectsCourseRanges() {
        UUID programUuid = UUID.randomUUID();
        Course first = course(3, 12);
        Course second = course(6, 15);
        when(programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid))
                .thenReturn(List.of(programCourse(first.getUuid()), programCourse(second.getUuid())));
        when(courseRepository.findByUuidIn(any())).thenReturn(List.of(first, second));
        when(lessonRepository.findByCourseUuidOrderByLessonNumberAsc(first.getUuid()))
                .thenReturn(List.of(lesson(lessonOne, true)));
        when(lessonRepository.findByCourseUuidOrderByLessonNumberAsc(second.getUuid()))
                .thenReturn(List.of(lesson(lessonTwo, true), lesson(UUID.randomUUID(), false)));

        AgeGroupScope programScope = ageGroups.forProgram(programUuid);

        assertThat(programScope.minAge()).isEqualTo(6);
        assertThat(programScope.maxAge()).isEqualTo(12);
        assertThat(programScope.lessonUuids()).containsExactly(lessonOne, lessonTwo);
    }

    @Test
    @DisplayName("courses with no common age range leave no room for any group")
    void disjointProgramRangesAreRefused() {
        AgeGroupScope disjoint = new AgeGroupScope(10, 8, List.of(lessonOne, lessonTwo), "this program");
        assertThatThrownBy(() -> ageGroups.validate(disjoint,
                List.of(group("Juniors", 8, 9))))
                .hasMessageContaining("share no common age range");
    }

    @Test
    @DisplayName("an empty list clears the stored groups without inserting any")
    void emptyReplaceClears() {
        UUID applicationUuid = UUID.randomUUID();
        ageGroups.replace(AgeGroupOwnerType.COURSE_TRAINING_APPLICATION, applicationUuid, List.of());
        verify(groupRepository).deleteForOwner(AgeGroupOwnerType.COURSE_TRAINING_APPLICATION, applicationUuid);
        verify(groupRepository, never()).saveAllAndFlush(any());
    }

    private AgeGroupRequest group(String name, int min, int max) {
        return new AgeGroupRequest(name, min, max, List.of(
                new LessonHoursRequest(lessonOne, new BigDecimal("0.5")),
                new LessonHoursRequest(lessonTwo, new BigDecimal("0.25"))));
    }

    private static Course course(Integer lower, Integer upper) {
        Course course = new Course();
        course.setUuid(UUID.randomUUID());
        course.setAgeLowerLimit(lower);
        course.setAgeUpperLimit(upper);
        return course;
    }

    private static ProgramCourse programCourse(UUID courseUuid) {
        ProgramCourse programCourse = new ProgramCourse();
        programCourse.setCourseUuid(courseUuid);
        return programCourse;
    }

    private static Lesson lesson(UUID uuid, boolean active) {
        Lesson lesson = new Lesson();
        lesson.setUuid(uuid);
        lesson.setActive(active);
        return lesson;
    }
}
