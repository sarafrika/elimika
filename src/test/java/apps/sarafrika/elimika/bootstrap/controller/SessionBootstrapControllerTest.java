package apps.sarafrika.elimika.bootstrap.controller;

import apps.sarafrika.elimika.bootstrap.dto.ActiveOrganisationDTO;
import apps.sarafrika.elimika.bootstrap.dto.RoleProfilesDTO;
import apps.sarafrika.elimika.bootstrap.dto.SessionBootstrapDTO;
import apps.sarafrika.elimika.bootstrap.service.SessionBootstrapService;
import apps.sarafrika.elimika.notifications.spi.DomainNotificationCounts;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SessionBootstrapController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(SessionBootstrapControllerTest.MockConfig.class)
class SessionBootstrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionBootstrapService sessionBootstrapService;

    @Autowired
    private DomainSecurityService domainSecurityService;

    @BeforeEach
    void setUp() {
        reset(sessionBootstrapService, domainSecurityService);
    }

    @Test
    void servesTheCallersBootstrapInSnakeCase() throws Exception {
        UUID userUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(userUuid);
        UserDTO user = new UserDTO(userUuid, "U-1", "Jane", null, "Doe", "jane@example.com", "jane", null, null,
                null, true, "kc-1", null, null, null, null, null, List.of("organisation_user"), List.of());
        when(sessionBootstrapService.bootstrap(userUuid)).thenReturn(new SessionBootstrapDTO(
                user,
                new RoleProfilesDTO(studentUuid, null, null),
                new ActiveOrganisationDTO(organisationUuid, "Acme", "admin", null, null, true),
                new WalletBalanceSummary(null, "KES", BigDecimal.ZERO),
                new UnreadNotificationSummary(4, 1, Map.of("organisation_user", new DomainNotificationCounts(4, 1)))));

        mockMvc.perform(get("/api/v1/me/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.uuid").value(userUuid.toString()))
                .andExpect(jsonPath("$.data.user.user_domain[0]").value("organisation_user"))
                .andExpect(jsonPath("$.data.profiles.student_uuid").value(studentUuid.toString()))
                .andExpect(jsonPath("$.data.profiles.course_creator_uuid").isEmpty())
                .andExpect(jsonPath("$.data.active_organisation.organisation_uuid").value(organisationUuid.toString()))
                .andExpect(jsonPath("$.data.wallet.currency_code").value("KES"))
                .andExpect(jsonPath("$.data.notifications.unread_count").value(4))
                .andExpect(jsonPath("$.data.notifications.by_domain.organisation_user.popup_count").value(1));
    }

    @Test
    void answersNotFoundWhenTheCallerHasNoUserRecord() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(null);

        mockMvc.perform(get("/api/v1/me/bootstrap")).andExpect(status().isNotFound());
        verify(sessionBootstrapService, never()).bootstrap(any());
    }

    static class MockConfig {
        @Bean
        SessionBootstrapService sessionBootstrapService() {
            return Mockito.mock(SessionBootstrapService.class);
        }

        @Bean
        DomainSecurityService domainSecurityService() {
            return Mockito.mock(DomainSecurityService.class);
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
