package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentCourseEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private TimetableService timetableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EnrollmentController(timetableService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getScheduledInstanceEnrollmentsForStudentDoesNotRequireDateParameters() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();

        EnrollmentDTO enrollment = new EnrollmentDTO(
                enrollmentUuid,
                instanceUuid,
                studentUuid,
                EnrollmentStatus.ENROLLED,
                null,
                null,
                null,
                null,
                null
        );

        when(timetableService.getEnrollmentsForStudent(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(enrollment), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/scheduled-instances", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student scheduled instance enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].uuid").value(enrollmentUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].scheduled_instance_uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));

        verify(timetableService).getEnrollmentsForStudent(eq(studentUuid), any(Pageable.class));
    }

    @Test
    void getClassEnrollmentsForStudentReturnsGroupedClassData() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();

        StudentClassEnrollmentSummaryDTO summary = new StudentClassEnrollmentSummaryDTO(
                classDefinitionUuid,
                "Java Fundamentals",
                UUID.randomUUID(),
                EnrollmentStatus.ENROLLED,
                3,
                null,
                null
        );

        when(timetableService.getClassEnrollmentsForStudent(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/classes", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student class enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].class_title").value("Java Fundamentals"))
                .andExpect(jsonPath("$.data.content[0].latest_enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.content[0].scheduled_instance_count").value(3))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));

        verify(timetableService).getClassEnrollmentsForStudent(eq(studentUuid), any(Pageable.class));
    }

    @Test
    void getCourseEnrollmentsForStudentReturnsCourseData() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        StudentCourseEnrollmentSummaryDTO summary = new StudentCourseEnrollmentSummaryDTO(
                UUID.randomUUID(),
                courseUuid,
                "Backend Engineering",
                "ACTIVE",
                null,
                null
        );

        when(timetableService.getCourseEnrollmentsForStudent(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/courses", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student course enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].course_uuid").value(courseUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].course_name").value("Backend Engineering"))
                .andExpect(jsonPath("$.data.content[0].enrollment_status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));

        verify(timetableService).getCourseEnrollmentsForStudent(eq(studentUuid), any(Pageable.class));
    }

    @Test
    void getEnrollmentOverviewForStudentReturnsOverallClassesAndCourses() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        when(timetableService.getClassEnrollmentsForStudent(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StudentClassEnrollmentSummaryDTO(
                                classDefinitionUuid,
                                "Java Fundamentals",
                                UUID.randomUUID(),
                                EnrollmentStatus.ENROLLED,
                                3,
                                null,
                                null
                        )),
                        PageRequest.of(0, 20),
                        1)
                );
        when(timetableService.getCourseEnrollmentsForStudent(eq(studentUuid), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StudentCourseEnrollmentSummaryDTO(
                                UUID.randomUUID(),
                                courseUuid,
                                "Backend Engineering",
                                "ACTIVE",
                                null,
                                null
                        )),
                        PageRequest.of(0, 20),
                        1)
        );

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/overview", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student enrollment overview retrieved successfully"))
                .andExpect(jsonPath("$.data.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.class_enrollments.content[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.class_enrollments.content[0].latest_enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.class_enrollments.metadata.totalElements").value(1))
                .andExpect(jsonPath("$.data.course_enrollments.content[0].course_uuid").value(courseUuid.toString()))
                .andExpect(jsonPath("$.data.course_enrollments.content[0].enrollment_status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.course_enrollments.metadata.totalElements").value(1));

        verify(timetableService).getClassEnrollmentsForStudent(eq(studentUuid), any(Pageable.class));
        verify(timetableService).getCourseEnrollmentsForStudent(eq(studentUuid), any(Pageable.class));
    }
}
