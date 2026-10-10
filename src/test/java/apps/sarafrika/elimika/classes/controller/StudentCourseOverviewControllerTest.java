package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewItemDTO;
import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewSessionDTO;
import apps.sarafrika.elimika.classes.service.StudentCourseOverviewService;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentCourseOverviewControllerTest {

    @Mock
    private StudentCourseOverviewService studentCourseOverviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentCourseOverviewController(studentCourseOverviewService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void returnsTheComposedOverviewInSnakeCase() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID classUuid = UUID.randomUUID();
        UUID sessionUuid = UUID.randomUUID();
        StudentCourseOverviewItemDTO item = new StudentCourseOverviewItemDTO(
                classUuid, "Grade 5 Piano", null, null, UUID.randomUUID(), EnrollmentStatus.ENROLLED, 6,
                UUID.randomUUID(), "Beginner Piano", null, UUID.randomUUID(), "Ms Wanjiru",
                new StudentCourseOverviewSessionDTO(sessionUuid, "Lesson 3", LocalDateTime.of(2031, 5, 5, 9, 0),
                        LocalDateTime.of(2031, 5, 5, 11, 0), "UTC", "ONLINE", null, null),
                UUID.randomUUID(), "ACTIVE", new BigDecimal("42.5"), 2, 1, null);
        when(studentCourseOverviewService.getCourseOverview(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/course-overview", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.enrollments.content[0].class_definition_uuid").value(classUuid.toString()))
                .andExpect(jsonPath("$.data.enrollments.content[0].course_name").value("Beginner Piano"))
                .andExpect(jsonPath("$.data.enrollments.content[0].instructor_name").value("Ms Wanjiru"))
                .andExpect(jsonPath("$.data.enrollments.content[0].next_session.scheduled_instance_uuid").value(sessionUuid.toString()))
                .andExpect(jsonPath("$.data.enrollments.content[0].progress_percentage").value(42.5))
                .andExpect(jsonPath("$.data.enrollments.content[0].pending_assignment_count").value(2))
                .andExpect(jsonPath("$.data.enrollments.content[0].pending_quiz_count").value(1))
                .andExpect(jsonPath("$.data.enrollments.metadata.totalElements").value(1));
    }
}
