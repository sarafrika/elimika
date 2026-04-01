package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClassDefinitionController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(ClassDefinitionControllerTest.MockConfig.class)
class ClassDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClassDefinitionServiceInterface classDefinitionService;

    @Autowired
    private TimetableService timetableService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private RequestAuditService requestAuditService;

    @BeforeEach
    void setUp() {
        reset(classDefinitionService, timetableService, userManagementService, requestAuditService);
    }

    @Test
    void createClassDefinitionForProgramCarriesNewClassMetadataFields() throws Exception {
        UUID programUuid = UUID.randomUUID();
        ClassDefinitionDTO request = sampleRequest(null, null, 30, "#1F6FEB");

        ClassDefinitionDTO responseDto = sampleRequest(
                null,
                programUuid,
                request.classReminderMinutes(),
                request.classColor()
        );

        when(classDefinitionService.createClassDefinition(any(ClassDefinitionDTO.class)))
                .thenReturn(new ClassDefinitionResponseDTO(responseDto));

        mockMvc.perform(post("/api/v1/classes/program/{programUuid}", programUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.class_definition.program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.class_definition.academic_period_start_date").value("2026-05-01"))
                .andExpect(jsonPath("$.data.class_definition.academic_period_end_date").value("2026-07-31"))
                .andExpect(jsonPath("$.data.class_definition.registration_period_start_date").value("2026-04-15"))
                .andExpect(jsonPath("$.data.class_definition.registration_period_end_date").value("2026-04-15"))
                .andExpect(jsonPath("$.data.class_definition.class_reminder_minutes").value(30))
                .andExpect(jsonPath("$.data.class_definition.class_color").value("#1F6FEB"));

        ArgumentCaptor<ClassDefinitionDTO> captor = ArgumentCaptor.forClass(ClassDefinitionDTO.class);
        verify(classDefinitionService).createClassDefinition(captor.capture());

        ClassDefinitionDTO forwarded = captor.getValue();
        assertEquals(programUuid, forwarded.programUuid());
        assertNull(forwarded.courseUuid());
        assertEquals(request.academicPeriodStartDate(), forwarded.academicPeriodStartDate());
        assertEquals(request.academicPeriodEndDate(), forwarded.academicPeriodEndDate());
        assertEquals(request.registrationPeriodStartDate(), forwarded.registrationPeriodStartDate());
        assertEquals(request.registrationPeriodEndDate(), forwarded.registrationPeriodEndDate());
        assertEquals(request.classReminderMinutes(), forwarded.classReminderMinutes());
        assertEquals(request.classColor(), forwarded.classColor());
    }

    @Test
    void createClassDefinitionRejectsInvalidAcademicPeriod() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                30,
                "#1F6FEB",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 15)
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    @Test
    void createClassDefinitionRejectsInvalidRegistrationPeriod() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                30,
                "#1F6FEB",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 15)
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    @Test
    void createClassDefinitionRejectsInvalidReminderMinutesAndColor() throws Exception {
        ClassDefinitionDTO request = sampleRequest(
                UUID.randomUUID(),
                null,
                -5,
                "blue"
        );

        mockMvc.perform(post("/api/v1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(classDefinitionService);
    }

    private ClassDefinitionDTO sampleRequest(UUID courseUuid,
                                             UUID programUuid,
                                             Integer classReminderMinutes,
                                             String classColor) {
        return sampleRequest(
                courseUuid,
                programUuid,
                classReminderMinutes,
                classColor,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 15)
        );
    }

    private ClassDefinitionDTO sampleRequest(UUID courseUuid,
                                             UUID programUuid,
                                             Integer classReminderMinutes,
                                             String classColor,
                                             LocalDate academicPeriodStartDate,
                                             LocalDate academicPeriodEndDate,
                                             LocalDate registrationPeriodStartDate,
                                             LocalDate registrationPeriodEndDate) {
        return new ClassDefinitionDTO(
                UUID.randomUUID(),
                "Data Science Cohort",
                "Applied analytics class",
                UUID.randomUUID(),
                UUID.randomUUID(),
                courseUuid,
                programUuid,
                new BigDecimal("240.00"),
                ClassVisibility.PUBLIC,
                SessionFormat.GROUP,
                LocalDateTime.of(2026, 5, 5, 9, 0),
                LocalDateTime.of(2026, 5, 5, 11, 0),
                academicPeriodStartDate,
                academicPeriodEndDate,
                registrationPeriodStartDate,
                registrationPeriodEndDate,
                classReminderMinutes,
                classColor,
                LocationType.HYBRID,
                "Nairobi HQ - Room 4",
                new BigDecimal("-1.292066"),
                new BigDecimal("36.821945"),
                "https://meet.google.com/abc-defg-hij",
                25,
                true,
                true,
                List.of(new ClassSessionTemplateDTO(
                        LocalDateTime.of(2026, 5, 5, 9, 0),
                        LocalDateTime.of(2026, 5, 5, 11, 0),
                        new ClassRecurrenceDTO(
                                ClassRecurrenceDTO.RecurrenceType.WEEKLY,
                                1,
                                "MONDAY,WEDNESDAY",
                                null,
                                LocalDate.of(2026, 7, 31),
                                8
                        ),
                        ConflictResolutionStrategy.FAIL
                )),
                null,
                null,
                null,
                null
        );
    }

    static class MockConfig {
        @Bean
        ClassDefinitionServiceInterface classDefinitionService() {
            return Mockito.mock(ClassDefinitionServiceInterface.class);
        }

        @Bean
        TimetableService timetableService() {
            return Mockito.mock(TimetableService.class);
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
