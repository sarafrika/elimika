package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobAssignmentRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClassMarketplaceJobController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(ClassMarketplaceJobControllerTest.MockConfig.class)
class ClassMarketplaceJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassMarketplaceJobServiceInterface classMarketplaceJobService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private RequestAuditService requestAuditService;

    @BeforeEach
    void setUp() {
        reset(classMarketplaceJobService, userManagementService, requestAuditService);
    }

    @Test
    void createJobReturnsCreatedAndForwardsRequest() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.createJob(any(ClassMarketplaceJobRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uuid").value(response.uuid().toString()))
                .andExpect(jsonPath("$.data.status").value("open"))
                .andExpect(jsonPath("$.data.course_uuid").value(request.courseUuid().toString()));

        ArgumentCaptor<ClassMarketplaceJobRequestDTO> captor =
                ArgumentCaptor.forClass(ClassMarketplaceJobRequestDTO.class);
        verify(classMarketplaceJobService).createJob(captor.capture());
        assertEquals(request.organisationUuid(), captor.getValue().organisationUuid());
        assertEquals(request.sessionTemplates().size(), captor.getValue().sessionTemplates().size());
    }

    @Test
    void createProgramJobReturnsCreatedAndForwardsRequest() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleProgramRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.createJob(any(ClassMarketplaceJobRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uuid").value(response.uuid().toString()))
                .andExpect(jsonPath("$.data.status").value("open"))
                .andExpect(jsonPath("$.data.program_uuid").value(request.programUuid().toString()))
                .andExpect(jsonPath("$.data.course_uuid").doesNotExist());

        ArgumentCaptor<ClassMarketplaceJobRequestDTO> captor =
                ArgumentCaptor.forClass(ClassMarketplaceJobRequestDTO.class);
        verify(classMarketplaceJobService).createJob(captor.capture());
        assertEquals(request.organisationUuid(), captor.getValue().organisationUuid());
        assertEquals(request.programUuid(), captor.getValue().programUuid());
    }

    @Test
    void assignInstructorReturnsConflictWhenSchedulingFails() throws Exception {
        UUID jobUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();

        when(classMarketplaceJobService.assignInstructor(any(UUID.class), any(ClassMarketplaceJobAssignmentRequestDTO.class)))
                .thenThrow(new SchedulingConflictException(
                        "Conflicts detected",
                        List.of(new ClassSchedulingConflictDTO(
                                LocalDateTime.of(2026, 5, 2, 9, 0),
                                LocalDateTime.of(2026, 5, 2, 12, 0),
                                List.of("Instructor has overlapping scheduled instances")
                        ))
                ));

        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/assignments", jobUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClassMarketplaceJobAssignmentRequestDTO(applicationUuid))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Scheduling conflicts detected"))
                .andExpect(jsonPath("$.error[0].reasons[0]").value("Instructor has overlapping scheduled instances"));
    }

    @Test
    void listJobsAcceptsLowercaseStatusFilter() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.listJobs(
                eq(request.organisationUuid()),
                eq(request.courseUuid()),
                isNull(),
                eq(ClassMarketplaceJobStatus.OPEN),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/classes/jobs")
                        .param("organisation_uuid", request.organisationUuid().toString())
                        .param("course_uuid", request.courseUuid().toString())
                        .param("status", "open")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(response.uuid().toString()))
                .andExpect(jsonPath("$.data.metadata.pageNumber").value(0))
                .andExpect(jsonPath("$.data.metadata.pageSize").value(20));

        verify(classMarketplaceJobService).listJobs(
                eq(request.organisationUuid()),
                eq(request.courseUuid()),
                isNull(),
                eq(ClassMarketplaceJobStatus.OPEN),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    void listJobsAcceptsProgramFilter() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleProgramRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.listJobs(
                eq(request.organisationUuid()),
                isNull(),
                eq(request.programUuid()),
                eq(ClassMarketplaceJobStatus.OPEN),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/classes/jobs")
                        .param("organisation_uuid", request.organisationUuid().toString())
                        .param("program_uuid", request.programUuid().toString())
                        .param("status", "open")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(response.uuid().toString()))
                .andExpect(jsonPath("$.data.content[0].program_uuid").value(request.programUuid().toString()))
                .andExpect(jsonPath("$.data.content[0].course_uuid").doesNotExist());

        verify(classMarketplaceJobService).listJobs(
                eq(request.organisationUuid()),
                isNull(),
                eq(request.programUuid()),
                eq(ClassMarketplaceJobStatus.OPEN),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    private ClassMarketplaceJobRequestDTO sampleRequest() {
        return new ClassMarketplaceJobRequestDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Weekend Data Analysis Bootcamp",
                "School advert for an approved class slot",
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 5, 1),
                30,
                "#1F6FEB",
                LocationType.HYBRID,
                "Nairobi Campus - Lab 2",
                new BigDecimal("-1.292066"),
                new BigDecimal("36.821945"),
                "https://meet.google.com/abc-defg-hij",
                24,
                true,
                new BigDecimal("240.00"),
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 5, 2, 9, 0),
                        LocalDateTime.of(2026, 5, 2, 12, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "SATURDAY",
                                null,
                                null,
                                6
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null
        );
    }

    private ClassMarketplaceJobRequestDTO sampleProgramRequest() {
        return new ClassMarketplaceJobRequestDTO(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Weekend Data Analysis Program",
                "School advert for an approved program class slot",
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 5, 2, 9, 0),
                LocalDateTime.of(2026, 5, 2, 12, 0),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 6, 6),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 5, 1),
                30,
                "#1F6FEB",
                LocationType.HYBRID,
                "Nairobi Campus - Lab 2",
                new BigDecimal("-1.292066"),
                new BigDecimal("36.821945"),
                "https://meet.google.com/abc-defg-hij",
                24,
                true,
                new BigDecimal("240.00"),
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 5, 2, 9, 0),
                        LocalDateTime.of(2026, 5, 2, 12, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "SATURDAY",
                                null,
                                null,
                                6
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null
        );
    }

    private ClassMarketplaceJobDTO sampleResponse(ClassMarketplaceJobRequestDTO request) {
        return new ClassMarketplaceJobDTO(
                UUID.randomUUID(),
                request.organisationUuid(),
                request.courseUuid(),
                request.programUuid(),
                request.title(),
                request.description(),
                request.trainingFee(),
                ClassMarketplaceJobStatus.OPEN,
                request.classVisibility(),
                request.sessionFormat(),
                request.defaultStartTime(),
                request.defaultEndTime(),
                request.academicPeriodStartDate(),
                request.academicPeriodEndDate(),
                request.registrationPeriodStartDate(),
                request.registrationPeriodEndDate(),
                request.classReminderMinutes(),
                request.classColor(),
                null,
                request.locationType(),
                request.locationName(),
                request.locationLatitude(),
                request.locationLongitude(),
                request.meetingLink(),
                request.maxParticipants(),
                request.allowWaitlist(),
                null,
                null,
                null,
                null,
                request.sessionTemplates(),
                null,
                null,
                null,
                null,
                null
        );
    }

    static class MockConfig {
        @Bean
        ClassMarketplaceJobServiceInterface classMarketplaceJobService() {
            return Mockito.mock(ClassMarketplaceJobServiceInterface.class);
        }

        @Bean
        UserManagementService userManagementService() {
            return Mockito.mock(UserManagementService.class);
        }

        @Bean
        RequestAuditService requestAuditService() {
            return Mockito.mock(RequestAuditService.class);
        }
    }
}
