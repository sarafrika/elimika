package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassAssessmentSchedulesDTO;
import apps.sarafrika.elimika.classes.service.ClassAssessmentScheduleService;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Loads the class-definition controller too, so the literal path is proven not to fall into GET /{uuid}. */
@WebMvcTest(value = {ClassAssessmentScheduleBatchController.class, ClassDefinitionController.class},
        properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import({ClassDefinitionControllerTest.MockConfig.class, ClassAssessmentScheduleBatchControllerTest.MockConfig.class,
        GlobalExceptionHandler.class})
class ClassAssessmentScheduleBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassAssessmentScheduleService classAssessmentScheduleService;

    @Autowired
    private ClassDefinitionServiceInterface classDefinitionService;

    @BeforeEach
    void setUp() {
        reset(classAssessmentScheduleService, classDefinitionService);
    }

    @Test
    void literalPathRoutesToTheBatchReadNotToGetClassByUuid() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(classAssessmentScheduleService.getAssessmentSchedules(List.of(first, second)))
                .thenReturn(new ClassAssessmentSchedulesDTO(List.of(), List.of()));

        mockMvc.perform(get("/api/v1/classes/assessment-schedules").param("class_uuids", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignment_schedules").isArray())
                .andExpect(jsonPath("$.data.quiz_schedules").isArray());

        verify(classAssessmentScheduleService).getAssessmentSchedules(List.of(first, second));
        verify(classDefinitionService, never()).getClassDefinition(any());
    }

    @Test
    void getClassByUuidStillResolvesToTheClassDefinitionController() throws Exception {
        UUID classUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/classes/{uuid}", classUuid));

        verify(classDefinitionService).getClassDefinition(classUuid);
        verify(classAssessmentScheduleService, never()).getAssessmentSchedules(any());
    }

    @Test
    void missingClassUuidsIsABadRequest() throws Exception {
        when(classAssessmentScheduleService.getAssessmentSchedules(any()))
                .thenThrow(new IllegalArgumentException("At least one class uuid must be requested"));

        mockMvc.perform(get("/api/v1/classes/assessment-schedules"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedBatchIsABadRequest() throws Exception {
        when(classAssessmentScheduleService.getAssessmentSchedules(any()))
                .thenThrow(new IllegalArgumentException("At most 100 class uuids may be requested at once"));

        mockMvc.perform(get("/api/v1/classes/assessment-schedules").param("class_uuids", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    static class MockConfig {
        @Bean
        ClassAssessmentScheduleService classAssessmentScheduleService() {
            return Mockito.mock(ClassAssessmentScheduleService.class);
        }
    }
}
