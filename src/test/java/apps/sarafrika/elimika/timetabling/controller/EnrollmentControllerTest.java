package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentCourseEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentEnrollmentOverviewDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new EnrollmentController(timetableService)).build();
    }

    @Test
    void getEnrollmentsForStudentDoesNotRequireDateParameters() throws Exception {
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

        when(timetableService.getEnrollmentsForStudent(studentUuid)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data[0].uuid").value(enrollmentUuid.toString()))
                .andExpect(jsonPath("$.data[0].scheduled_instance_uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data[0].student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data[0].status").value("ENROLLED"));

        verify(timetableService).getEnrollmentsForStudent(studentUuid);
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

        when(timetableService.getClassEnrollmentsForStudent(studentUuid)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/classes", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student class enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data[0].class_title").value("Java Fundamentals"))
                .andExpect(jsonPath("$.data[0].latest_enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data[0].scheduled_instance_count").value(3));

        verify(timetableService).getClassEnrollmentsForStudent(studentUuid);
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

        when(timetableService.getCourseEnrollmentsForStudent(studentUuid)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/courses", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student course enrollments retrieved successfully"))
                .andExpect(jsonPath("$.data[0].course_uuid").value(courseUuid.toString()))
                .andExpect(jsonPath("$.data[0].course_name").value("Backend Engineering"))
                .andExpect(jsonPath("$.data[0].enrollment_status").value("ACTIVE"));

        verify(timetableService).getCourseEnrollmentsForStudent(studentUuid);
    }

    @Test
    void getEnrollmentOverviewForStudentReturnsOverallClassesAndCourses() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();

        StudentEnrollmentOverviewDTO overview = new StudentEnrollmentOverviewDTO(
                studentUuid,
                List.of(new StudentClassEnrollmentSummaryDTO(
                        classDefinitionUuid,
                        "Java Fundamentals",
                        UUID.randomUUID(),
                        EnrollmentStatus.ENROLLED,
                        3,
                        null,
                        null
                )),
                List.of(new StudentCourseEnrollmentSummaryDTO(
                        UUID.randomUUID(),
                        courseUuid,
                        "Backend Engineering",
                        "ACTIVE",
                        null,
                        null
                ))
        );

        when(timetableService.getEnrollmentOverviewForStudent(studentUuid)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/enrollment/student/{studentUuid}/overview", studentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student enrollment overview retrieved successfully"))
                .andExpect(jsonPath("$.data.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.class_enrollments[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data.class_enrollments[0].latest_enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.course_enrollments[0].course_uuid").value(courseUuid.toString()))
                .andExpect(jsonPath("$.data.course_enrollments[0].enrollment_status").value("ACTIVE"));

        verify(timetableService).getEnrollmentOverviewForStudent(studentUuid);
    }
}
