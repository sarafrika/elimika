package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassReviewService;
import apps.sarafrika.elimika.shared.security.ClassAccessSecurityService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentVisibilityService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The class-takeover this guard exists for: the update route accepts
 * {@code default_instructor_uuid}, so while it was unguarded, naming yourself in the body of any
 * class made you its instructor — and from there its session calendar and its deletion were yours
 * too. These cover the three routes that hand an existing class over.
 * <p>
 * The filter chain here permits every request on purpose, so the {@code @PreAuthorize} on the
 * handler is the only thing that can refuse one.
 */
@WebMvcTest(value = ClassDefinitionController.class, properties = "app.keycloak.realm=test-realm")
@ExtendWith(SpringExtension.class)
@Import({ClassDefinitionAuthorizationTest.MockConfig.class, ClassDefinitionAuthorizationTest.MethodSecurityConfig.class})
@DisplayName("Class definition mutation authorization")
class ClassDefinitionAuthorizationTest {

    private static final UUID CLASS_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String VALID_UPDATE_BODY = """
            {
              "title": "Introduction to Java",
              "default_instructor_uuid": "55555555-5555-5555-5555-555555555555",
              "class_visibility": "PUBLIC",
              "session_format": "GROUP",
              "default_start_time": "2026-03-02T09:00:00",
              "default_end_time": "2026-03-02T10:30:00",
              "location_type": "ONLINE"
            }
            """;

    private static final String VALID_SESSION_TEMPLATE_BODY = """
            {
              "start_time": "2026-03-02T09:00:00",
              "end_time": "2026-03-02T10:30:00"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassDefinitionServiceInterface classDefinitionService;

    @Autowired
    private DomainSecurityService domainSecurityService;

    @BeforeEach
    void setUp() {
        reset(classDefinitionService, domainSecurityService);
    }

    @Test
    void anOutsiderCannotMakeThemselvesTheInstructorOfSomebodyElsesClass() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(put("/api/v1/classes/{uuid}", CLASS_UUID)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isForbidden());

        verify(classDefinitionService, never()).updateClassDefinition(any(), any());
    }

    @Test
    void anOutsiderCannotWriteSessionsIntoSomebodyElsesClass() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(post("/api/v1/classes/{uuid}/session-templates", CLASS_UUID)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SESSION_TEMPLATE_BODY))
                .andExpect(status().isForbidden());

        verify(classDefinitionService, never()).addSessionTemplate(any(), any());
    }

    @Test
    void anOutsiderCannotDeactivateSomebodyElsesClass() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/classes/{uuid}", CLASS_UUID).with(jwt()))
                .andExpect(status().isForbidden());

        verify(classDefinitionService, never()).deactivateClassDefinition(any());
    }

    @Test
    void whoeverRunsTheClassMayDeactivateIt() throws Exception {
        when(domainSecurityService.canManageClass(CLASS_UUID)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/classes/{uuid}", CLASS_UUID).with(jwt()))
                .andExpect(status().isOk());

        verify(classDefinitionService).deactivateClassDefinition(CLASS_UUID);
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
        ClassDefinitionServiceInterface classDefinitionService() {
            return Mockito.mock(ClassDefinitionServiceInterface.class);
        }

        @Bean
        DomainSecurityService domainSecurityService() {
            return Mockito.mock(DomainSecurityService.class);
        }

        @Bean
        TimetableService timetableService() {
            return Mockito.mock(TimetableService.class);
        }

        @Bean
        ClassReviewService classReviewService() {
            return Mockito.mock(ClassReviewService.class);
        }

        @Bean
        StorageService storageService() {
            return Mockito.mock(StorageService.class);
        }

        @Bean
        MediaServeService mediaServeService(StorageService storageService) {
            return new MediaServeService(storageService);
        }

        @Bean
        StorageProperties storageProperties() {
            StorageProperties storageProperties = new StorageProperties();
            StorageProperties.Folders folders = new StorageProperties.Folders();
            folders.setClassThumbnails("class_thumbnails");
            folders.setClassPromotionalVideos("class_promotional_videos");
            storageProperties.setFolders(folders);
            return storageProperties;
        }

        @Bean
        ClassAccessSecurityService classAccessSecurityService() {
            return Mockito.mock(ClassAccessSecurityService.class);
        }

        @Bean
        EnrollmentVisibilityService enrollmentVisibilityService() {
            return Mockito.mock(EnrollmentVisibilityService.class);
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
