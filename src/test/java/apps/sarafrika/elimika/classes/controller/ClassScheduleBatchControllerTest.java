package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassScheduleBatchService;
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
@WebMvcTest(value = {ClassScheduleBatchController.class, ClassDefinitionController.class},
        properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import({ClassDefinitionControllerTest.MockConfig.class, ClassScheduleBatchControllerTest.MockConfig.class,
        GlobalExceptionHandler.class})
class ClassScheduleBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassScheduleBatchService classScheduleBatchService;

    @Autowired
    private ClassDefinitionServiceInterface classDefinitionService;

    @BeforeEach
    void setUp() {
        reset(classScheduleBatchService, classDefinitionService);
    }

    @Test
    void literalPathRoutesToTheBatchReadNotToGetClassByUuid() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(classScheduleBatchService.getSchedules(List.of(first, second))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/classes/schedules").param("class_uuids", first + "," + second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(classScheduleBatchService).getSchedules(List.of(first, second));
        verify(classDefinitionService, never()).getClassDefinition(any());
    }

    @Test
    void missingClassUuidsIsABadRequest() throws Exception {
        when(classScheduleBatchService.getSchedules(any()))
                .thenThrow(new IllegalArgumentException("At least one class uuid must be requested"));

        mockMvc.perform(get("/api/v1/classes/schedules"))
                .andExpect(status().isBadRequest());
    }

    static class MockConfig {
        @Bean
        ClassScheduleBatchService classScheduleBatchService() {
            return Mockito.mock(ClassScheduleBatchService.class);
        }
    }
}
