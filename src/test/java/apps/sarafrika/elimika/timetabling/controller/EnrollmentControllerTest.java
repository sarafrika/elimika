package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
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
}
