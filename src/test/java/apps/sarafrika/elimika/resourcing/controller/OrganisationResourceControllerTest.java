package apps.sarafrika.elimika.resourcing.controller;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceCalendarDTO;
import apps.sarafrika.elimika.resourcing.service.OrganisationResourceServiceInterface;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrganisationResourceController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import({OrganisationResourceControllerTest.MockConfig.class, GlobalExceptionHandler.class})
class OrganisationResourceControllerTest {

    private static final UUID ORG_UUID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 1, 5);
    private static final LocalDate END = LocalDate.of(2026, 1, 11);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationResourceServiceInterface resourceService;

    @BeforeEach
    void setUp() {
        reset(resourceService);
    }

    @Test
    void literalCalendarPathRoutesToTheOrganisationWideRead() throws Exception {
        UUID resourceUuid = UUID.randomUUID();
        when(resourceService.getCalendars(ORG_UUID, START, END, List.of("HOLD", "CONFIRMED")))
                .thenReturn(List.of(new OrganisationResourceCalendarDTO(
                        resourceUuid, "Physics Lab", ResourceType.VENUE, List.of())));

        mockMvc.perform(get("/api/v1/organisations/{org}/resources/calendar", ORG_UUID)
                        .param("start_date", "2026-01-05")
                        .param("end_date", "2026-01-11")
                        .param("entry_types", "HOLD,CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resource_uuid").value(resourceUuid.toString()))
                .andExpect(jsonPath("$.data[0].resource_name").value("Physics Lab"))
                .andExpect(jsonPath("$.data[0].resource_type").exists())
                .andExpect(jsonPath("$.data[0].entries").isArray());

        verify(resourceService, never()).getResource(any(), any());
        verify(resourceService, never()).getCalendar(any(), any(), any(), any());
    }

    @Test
    void perResourceCalendarStillResolvesByUuid() throws Exception {
        UUID resourceUuid = UUID.randomUUID();
        when(resourceService.getCalendar(ORG_UUID, resourceUuid, START, END)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/organisations/{org}/resources/{resource}/calendar", ORG_UUID, resourceUuid)
                        .param("start_date", "2026-01-05")
                        .param("end_date", "2026-01-11"))
                .andExpect(status().isOk());

        verify(resourceService).getCalendar(ORG_UUID, resourceUuid, START, END);
        verify(resourceService, never()).getCalendars(any(), any(), any(), any());
    }

    @Test
    void entryTypesAreOptional() throws Exception {
        when(resourceService.getCalendars(eq(ORG_UUID), eq(START), eq(END), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/organisations/{org}/resources/calendar", ORG_UUID)
                        .param("start_date", "2026-01-05")
                        .param("end_date", "2026-01-11"))
                .andExpect(status().isOk());
    }

    @Test
    void outsidersAreRefusedLikeTheSingleResourceCalendar() throws Exception {
        when(resourceService.getCalendars(any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("not a manager"));

        mockMvc.perform(get("/api/v1/organisations/{org}/resources/calendar", ORG_UUID)
                        .param("start_date", "2026-01-05")
                        .param("end_date", "2026-01-11"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anInvalidRangeIsABadRequest() throws Exception {
        when(resourceService.getCalendars(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Calendar range cannot exceed 400 days"));

        mockMvc.perform(get("/api/v1/organisations/{org}/resources/calendar", ORG_UUID)
                        .param("start_date", "2026-01-01")
                        .param("end_date", "2028-01-01"))
                .andExpect(status().isBadRequest());
    }

    static class MockConfig {
        @Bean
        OrganisationResourceServiceInterface resourceService() {
            return Mockito.mock(OrganisationResourceServiceInterface.class);
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
