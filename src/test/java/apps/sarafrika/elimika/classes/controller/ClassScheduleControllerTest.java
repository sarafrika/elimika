package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.service.ClassAssessmentScheduleService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the annotations on the assessment-scheduling routes actually gate them, and that the two
 * bands are distinct: reading what a class has due admits the cohort, changing it does not.
 * <p>
 * The filter chain here permits every request on purpose, so the {@code @PreAuthorize} on the
 * handler is the only thing that can refuse one.
 */
@WebMvcTest(value = ClassScheduleController.class, properties = "app.keycloak.realm=test-realm")
@ExtendWith(SpringExtension.class)
@Import({ClassScheduleControllerTest.MockConfig.class, ClassScheduleControllerTest.MethodSecurityConfig.class})
@DisplayName("Class assessment schedule authorization")
class ClassScheduleControllerTest {

    private static final UUID CLASS_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SCHEDULE_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassAssessmentScheduleService classAssessmentScheduleService;

    @Autowired
    private DomainSecurityService domainSecurityService;

    @BeforeEach
    void setUp() {
        reset(classAssessmentScheduleService, domainSecurityService);
    }

    @Test
    void aStrangerToTheClassCannotListWhatItHasDue() throws Exception {
        when(domainSecurityService.canViewClassSchedule(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(get("/api/v1/classes/{classUuid}/assignments", CLASS_UUID).with(jwt()))
                .andExpect(status().isForbidden());

        verify(classAssessmentScheduleService, never()).getAssignmentSchedules(any());
    }

    @Test
    void aStrangerToTheClassCannotListItsQuizzes() throws Exception {
        when(domainSecurityService.canViewClassSchedule(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(get("/api/v1/classes/{classUuid}/quizzes", CLASS_UUID).with(jwt()))
                .andExpect(status().isForbidden());

        verify(classAssessmentScheduleService, never()).getQuizSchedules(any());
    }

    @Test
    void someoneWithAStakeInTheClassMayListWhatItHasDue() throws Exception {
        when(domainSecurityService.canViewClassSchedule(CLASS_UUID)).thenReturn(true);
        when(classAssessmentScheduleService.getAssignmentSchedules(CLASS_UUID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/classes/{classUuid}/assignments", CLASS_UUID).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void beingAbleToReadTheScheduleIsNotBeingAbleToMoveIt() throws Exception {
        // The enrolled learner: they see the deadline, they do not get to change it.
        when(domainSecurityService.canViewClassSchedule(CLASS_UUID)).thenReturn(true);
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(post("/api/v1/classes/{classUuid}/assignments", CLASS_UUID)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(classAssessmentScheduleService, never()).createAssignmentSchedule(any(), any());
    }

    @Test
    void aStrangerToTheClassCannotDeleteItsSchedule() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/classes/{classUuid}/assignments/{scheduleUuid}", CLASS_UUID, SCHEDULE_UUID)
                        .with(jwt()))
                .andExpect(status().isForbidden());

        verify(classAssessmentScheduleService, never()).deleteAssignmentSchedule(any(), any());
    }

    @Test
    void whoeverRunsTheClassMayDeleteItsSchedule() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/classes/{classUuid}/assignments/{scheduleUuid}", CLASS_UUID, SCHEDULE_UUID)
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(classAssessmentScheduleService).deleteAssignmentSchedule(CLASS_UUID, SCHEDULE_UUID);
    }

    /**
     * Method security is switched on by {@code SecurityConfiguration}, which a web slice does not
     * load. The chain permits everything so that the handler annotations are the only gate.
     */
    @EnableMethodSecurity(securedEnabled = true)
    static class MethodSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .build();
        }
    }

    static class MockConfig {
        @Bean
        ClassAssessmentScheduleService classAssessmentScheduleService() {
            return Mockito.mock(ClassAssessmentScheduleService.class);
        }

        @Bean
        DomainSecurityService domainSecurityService() {
            return Mockito.mock(DomainSecurityService.class);
        }

        @Bean
        RequestAuditService requestAuditService() {
            return Mockito.mock(RequestAuditService.class);
        }

        @Bean
        UserManagementService userManagementService() {
            return Mockito.mock(UserManagementService.class);
        }
    }
}
