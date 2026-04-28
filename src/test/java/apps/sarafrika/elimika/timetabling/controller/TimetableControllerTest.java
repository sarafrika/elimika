package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void startScheduledInstanceReturnsUpdatedScheduledInstance() throws Exception {
        UUID instanceUuid = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 28, 9, 5);
        ScheduledInstanceDTO response = scheduledInstance(instanceUuid, SchedulingStatus.ONGOING, startedAt, null);

        when(timetableService.startScheduledInstance(instanceUuid)).thenReturn(response);

        mockMvc.perform(post("/api/v1/timetable/schedule/{instanceUuid}/start", instanceUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Class started successfully"))
                .andExpect(jsonPath("$.data.uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.started_at[0]").value(2026))
                .andExpect(jsonPath("$.data.started_at[1]").value(4))
                .andExpect(jsonPath("$.data.started_at[2]").value(28));

        verify(timetableService).startScheduledInstance(instanceUuid);
    }

    @Test
    void endScheduledInstanceReturnsUpdatedScheduledInstance() throws Exception {
        UUID instanceUuid = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 28, 9, 5);
        LocalDateTime concludedAt = LocalDateTime.of(2026, 4, 28, 10, 35);
        ScheduledInstanceDTO response = scheduledInstance(instanceUuid, SchedulingStatus.COMPLETED, startedAt, concludedAt);

        when(timetableService.endScheduledInstance(instanceUuid)).thenReturn(response);

        mockMvc.perform(post("/api/v1/timetable/schedule/{instanceUuid}/end", instanceUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Class ended successfully"))
                .andExpect(jsonPath("$.data.uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.concluded_at[0]").value(2026))
                .andExpect(jsonPath("$.data.concluded_at[1]").value(4))
                .andExpect(jsonPath("$.data.concluded_at[2]").value(28));

        verify(timetableService).endScheduledInstance(instanceUuid);
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

    private ScheduledInstanceDTO scheduledInstance(
            UUID instanceUuid,
            SchedulingStatus status,
            LocalDateTime startedAt,
            LocalDateTime concludedAt
    ) {
        return new ScheduledInstanceDTO(
                instanceUuid,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 4, 28, 9, 0),
                LocalDateTime.of(2026, 4, 28, 10, 30),
                "UTC",
                "Data Science Cohort",
                "ONLINE",
                null,
                null,
                null,
                25,
                status,
                null,
                startedAt,
                concludedAt,
                null,
                null,
                null,
                null
        );
    }
}
