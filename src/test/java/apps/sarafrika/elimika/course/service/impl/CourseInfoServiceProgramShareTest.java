package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("A program splits revenue by its courses' average creator share")
class CourseInfoServiceProgramShareTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ProgramCourseRepository programCourseRepository;

    @InjectMocks
    private CourseInfoServiceImpl service;

    private final UUID program = UUID.randomUUID();

    @Test
    @DisplayName("30% and 50% creator shares average to 40/60; a course without a split is ignored")
    void averagesTheCreatorShare() {
        List<UUID> courses = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(programCourseRepository.findCourseUuidsByProgramUuidIn(List.of(program))).thenReturn(courses);
        when(courseRepository.findByUuidIn(courses)).thenReturn(List.of(course("30.00"), course("50.00"), course(null)));

        RevenueShare share = service.getProgramRevenueShare(program).orElseThrow();

        assertThat(share.creatorSharePercentage()).isEqualByComparingTo("40.00");
        assertThat(share.instructorSharePercentage()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("a program whose courses have no split has none either")
    void noSplit() {
        when(programCourseRepository.findCourseUuidsByProgramUuidIn(List.of(program))).thenReturn(List.of());
        when(courseRepository.findByUuidIn(List.of())).thenReturn(List.of());

        assertThat(service.getProgramRevenueShare(program)).isEmpty();
    }

    private static Course course(String creatorShare) {
        Course course = new Course();
        course.setCreatorSharePercentage(creatorShare == null ? null : new BigDecimal(creatorShare));
        return course;
    }
}
