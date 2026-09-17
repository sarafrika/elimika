package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobEligibilityDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequestDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSchedulingConflictDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
                .andExpect(jsonPath("$.data.course_uuid").value(request.courseUuid().toString()))
                .andExpect(jsonPath("$.data.branch_uuid").value(request.branchUuid().toString()))
                .andExpect(jsonPath("$.data.branch_name").value("Main Campus"))
                .andExpect(jsonPath("$.data.application_count").value(3))
                .andExpect(jsonPath("$.data.hired_instructor_uuid").doesNotExist());

        ArgumentCaptor<ClassMarketplaceJobRequestDTO> captor =
                ArgumentCaptor.forClass(ClassMarketplaceJobRequestDTO.class);
        verify(classMarketplaceJobService).createJob(captor.capture());
        assertEquals(request.organisationUuid(), captor.getValue().organisationUuid());
        assertEquals(request.sessionTemplates().size(), captor.getValue().sessionTemplates().size());
        assertEquals(request.branchUuid(), captor.getValue().branchUuid());
    }

    @Test
    void createJobWithoutBranchReturns400() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();
        com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.valueToTree(request);
        body.remove("branch_uuid");

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.branch_uuid").value("branch_uuid is required"));

        verify(classMarketplaceJobService, Mockito.never()).createJob(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rate_basis", "sale_price", "instructor_pay"})
    void createJobWithoutPricingTermsReturns400(String field) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.valueToTree(sampleRequest());
        body.remove(field);

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error." + field).value(field + " is required"));

        verify(classMarketplaceJobService, Mockito.never()).createJob(any());
    }

    @Test
    void createJobWithZeroInstructorPayReturns400() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.valueToTree(sampleRequest());
        body.put("instructor_pay", 0);

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.instructor_pay").value("Instructor pay must be greater than zero"));

        verify(classMarketplaceJobService, Mockito.never()).createJob(any());
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
    void createClassForJobReturnsConflictWhenSchedulingFails() throws Exception {
        UUID jobUuid = UUID.randomUUID();

        when(classMarketplaceJobService.createClassForJob(any(UUID.class)))
                .thenThrow(new SchedulingConflictException(
                        "Conflicts detected",
                        List.of(new ClassSchedulingConflictDTO(
                                LocalDateTime.of(2026, 5, 2, 9, 0),
                                LocalDateTime.of(2026, 5, 2, 12, 0),
                                List.of("Instructor has overlapping scheduled instances")
                        ))
                ));

        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/class", jobUuid))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Scheduling conflicts detected"))
                .andExpect(jsonPath("$.error[0].reasons[0]").value("Instructor has overlapping scheduled instances"));
    }

    @Test
    void hireActionReachesTheHireMethodAndAssignIsGone() throws Exception {
        UUID jobUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/applications/{applicationUuid}", jobUuid, applicationUuid)
                        .param("action", "hire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Applicant hired successfully"));
        verify(classMarketplaceJobService).hireApplication(eq(jobUuid), eq(applicationUuid), any());

        // The assign endpoint is retired: no client may reach an assignment except by creating the class.
        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/assignments", jobUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"application_uuid\":\"" + applicationUuid + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hireRefusedForAScheduleClashReturns409WithTheClashingWindows() throws Exception {
        UUID jobUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();

        when(classMarketplaceJobService.hireApplication(eq(jobUuid), eq(applicationUuid), any()))
                .thenThrow(new SchedulingConflictException("Clashes", List.of(
                        new ClassSchedulingConflictDTO(
                                LocalDateTime.of(2026, 5, 2, 9, 0),
                                LocalDateTime.of(2026, 5, 2, 12, 0),
                                List.of("Instructor is already committed to another class job in this window")),
                        new ClassSchedulingConflictDTO(
                                LocalDateTime.of(2026, 5, 9, 9, 0),
                                LocalDateTime.of(2026, 5, 9, 12, 0),
                                List.of("Instructor is marked unavailable for this window")))));

        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/applications/{applicationUuid}", jobUuid, applicationUuid)
                        .param("action", "hire"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Schedule conflicts detected"))
                .andExpect(jsonPath("$.error.length()").value(2))
                .andExpect(jsonPath("$.error[0].requested_start").value("2026-05-02T09:00:00"))
                .andExpect(jsonPath("$.error[0].requested_end").value("2026-05-02T12:00:00"))
                .andExpect(jsonPath("$.error[0].reasons[0]")
                        .value("Instructor is already committed to another class job in this window"))
                .andExpect(jsonPath("$.error[1].requested_start").value("2026-05-09T09:00:00"));
    }

    @Test
    void hireRefusedForTheInstructorsRateReturns409WithTheReason() throws Exception {
        UUID jobUuid = UUID.randomUUID();
        UUID applicationUuid = UUID.randomUUID();
        when(classMarketplaceJobService.hireApplication(eq(jobUuid), eq(applicationUuid), any()))
                .thenThrow(new IllegalStateException(
                        "Jane Mwangi's approved rate of KES 300.00 per day is above this job's pay of KES 240.00."));

        mockMvc.perform(post("/api/v1/classes/jobs/{jobUuid}/applications/{applicationUuid}", jobUuid, applicationUuid)
                        .param("action", "hire"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Jane Mwangi's approved rate of KES 300.00 per day is above this job's pay of KES 240.00."));
    }

    @Test
    void eligibilityCarriesTheInstructorsRateForTheJob() throws Exception {
        UUID jobUuid = UUID.randomUUID();
        when(classMarketplaceJobService.getMyJobEligibility(jobUuid))
                .thenReturn(new ClassMarketplaceJobEligibilityDTO(jobUuid, false, true, true, false, new BigDecimal("300.00"),
                        false, null, false, true, null,
                        "Your approved rate of KES 300.00 per day is above this job's pay of KES 240.00."));

        mockMvc.perform(get("/api/v1/classes/jobs/{jobUuid}/eligibility", jobUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.job_uuid").value(jobUuid.toString()))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.rate_ok").value(false))
                .andExpect(jsonPath("$.data.approved_rate").value(300.00))
                .andExpect(jsonPath("$.data.reason")
                        .value("Your approved rate of KES 300.00 per day is above this job's pay of KES 240.00."));
    }

    @Test
    void batchEligibilityBindsTheCommaSeparatedJobsAndIsNotMistakenForAJobUuid() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(classMarketplaceJobService.getMyJobsEligibility(List.of(first, second))).thenReturn(List.of(
                new ClassMarketplaceJobEligibilityDTO(first, true, true, true, true, new BigDecimal("200.00"),
                        false, null, false, true, null, null),
                new ClassMarketplaceJobEligibilityDTO(second, false, true, false, false, null,
                        false, null, false, true, null, "You are not approved to deliver this course.")));

        mockMvc.perform(get("/api/v1/classes/jobs/eligibility").param("job_uuids", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].job_uuid").value(first.toString()))
                .andExpect(jsonPath("$.data[0].eligible").value(true))
                .andExpect(jsonPath("$.data[1].job_uuid").value(second.toString()))
                .andExpect(jsonPath("$.data[1].training_approved").value(false));
        verify(classMarketplaceJobService, Mockito.never()).getJob(any());
    }

    @Test
    void batchEligibilityAboveTheCapReturns400() throws Exception {
        when(classMarketplaceJobService.getMyJobsEligibility(any()))
                .thenThrow(new IllegalArgumentException("At most 50 job_uuids can be checked in one call; 51 were sent."));

        mockMvc.perform(get("/api/v1/classes/jobs/eligibility").param("job_uuids", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJobWhosePreferredInstructorClashesReturns409WithTheClashingWindows() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();

        when(classMarketplaceJobService.createJob(any(ClassMarketplaceJobRequestDTO.class)))
                .thenThrow(new SchedulingConflictException("Clashes", List.of(
                        new ClassSchedulingConflictDTO(
                                LocalDateTime.of(2026, 5, 16, 9, 0),
                                LocalDateTime.of(2026, 5, 16, 12, 0),
                                List.of("Instructor already has a scheduled session or blocked time overlapping this window")))));

        mockMvc.perform(post("/api/v1/classes/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Schedule conflicts detected"))
                .andExpect(jsonPath("$.error[0].requested_start").value("2026-05-16T09:00:00"));
    }

    @Test
    void listJobsAcceptsLowercaseStatusFilter() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.listJobs(
                eq(request.organisationUuid()),
                eq(request.courseUuid()),
                isNull(),
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
                isNull(),
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
                isNull(),
                eq(ClassMarketplaceJobStatus.OPEN),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    void listJobsAcceptsBranchFilter() throws Exception {
        ClassMarketplaceJobRequestDTO request = sampleRequest();
        ClassMarketplaceJobDTO response = sampleResponse(request);

        when(classMarketplaceJobService.listJobs(
                eq(request.organisationUuid()),
                isNull(),
                isNull(),
                eq(request.branchUuid()),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/classes/jobs")
                        .param("organisation_uuid", request.organisationUuid().toString())
                        .param("branch_uuid", request.branchUuid().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].branch_uuid").value(request.branchUuid().toString()))
                .andExpect(jsonPath("$.data.content[0].branch_name").value("Main Campus"));

        verify(classMarketplaceJobService).listJobs(
                eq(request.organisationUuid()),
                isNull(),
                isNull(),
                eq(request.branchUuid()),
                isNull(),
                any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    void jobResourceRequestIgnoresReadOnlyResourceLabels() throws Exception {
        UUID resourceUuid = UUID.randomUUID();

        apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobResourceDTO resource = objectMapper.readValue(
                "{\"resource_uuid\":\"" + resourceUuid + "\",\"quantity\":2,"
                        + "\"resource_name\":\"Spoofed\",\"resource_type\":\"VENUE\"}",
                apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobResourceDTO.class);

        assertEquals(resourceUuid, resource.resourceUuid());
        assertEquals(2, resource.quantity());
        assertEquals(null, resource.resourceName());
        assertEquals(null, resource.resourceType());
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
                new BigDecimal("240.00"),
                RateBasis.PER_HOUR,
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
                null,
                ClassServiceType.GROUP,
                UUID.randomUUID(),
                List.of("Grade 1", "Grade 2"),
                null,
                null,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                UUID.randomUUID()
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
                new BigDecimal("240.00"),
                RateBasis.PER_HOUR,
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
                null,
                ClassServiceType.GROUP,
                UUID.randomUUID(),
                List.of("Grade 1", "Grade 2"),
                null,
                null,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                UUID.randomUUID()
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
                request.salePrice(),
                request.instructorPay(),
                request.rateBasis(),
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
                null,
                request.serviceType(),
                request.preferredInstructorUuid(),
                request.targetGroups(),
                request.targetGroupUuids(),
                request.categoryUuid(),
                request.remindStudents(),
                request.remindInstructor(),
                request.remindViaEmail(),
                request.remindViaSms(),
                request.remindViaPush(),
                request.branchUuid(),
                "Main Campus",
                3L,
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
