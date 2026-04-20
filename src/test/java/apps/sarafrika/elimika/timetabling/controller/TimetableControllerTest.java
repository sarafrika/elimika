package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimetableControllerTest {

    @Mock
    private TimetableService timetableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TimetableController(timetableService)).build();
    }

    @Test
    void getStudentScheduleRequiresDateRangeParameters() throws Exception {
        UUID studentUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/timetable/student/{studentUuid}", studentUuid))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(timetableService);
    }

    @Test
    void getStudentScheduleReturnsSchedulePayloadFromTimetableRoute() throws Exception {
        UUID studentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        StudentScheduleDTO schedule = new StudentScheduleDTO(
                enrollmentUuid,
                instanceUuid,
                classDefinitionUuid,
                instructorUuid,
                "Data Science Cohort",
                LocalDateTime.of(2026, 4, 21, 9, 0),
                LocalDateTime.of(2026, 4, 21, 11, 0),
                "UTC",
                "ONLINE",
                "Virtual Classroom",
                null,
                null,
                SchedulingStatus.SCHEDULED,
                EnrollmentStatus.ENROLLED,
                null
        );

        when(timetableService.getScheduleForStudent(
                studentUuid,
                java.time.LocalDate.of(2026, 4, 21),
                java.time.LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(schedule));

        mockMvc.perform(get("/api/v1/timetable/student/{studentUuid}", studentUuid)
                        .param("start", "2026-04-21")
                        .param("end", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student schedule retrieved successfully"))
                .andExpect(jsonPath("$.data[0].enrollment_uuid").value(enrollmentUuid.toString()))
                .andExpect(jsonPath("$.data[0].scheduled_instance_uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data[0].class_definition_uuid").value(classDefinitionUuid.toString()))
                .andExpect(jsonPath("$.data[0].instructor_uuid").value(instructorUuid.toString()))
                .andExpect(jsonPath("$.data[0].enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data[0].scheduling_status").value("SCHEDULED"));

        verify(timetableService).getScheduleForStudent(
                studentUuid,
                java.time.LocalDate.of(2026, 4, 21),
                java.time.LocalDate.of(2026, 4, 30)
        );
    }
}
