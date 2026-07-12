package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import apps.sarafrika.elimika.tenancy.services.UserService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrganisationController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(OrganisationControllerTest.MockConfig.class)
class OrganisationControllerTest {

    private static final UUID ORG_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationService organisationService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrainingBranchService trainingBranchService;

    @BeforeEach
    void setUp() {
        reset(organisationService, userService, trainingBranchService);
    }

    @Test
    void getOrganisationStatisticsReturnsOrganisationScopedCounts() throws Exception {
        when(organisationService.getOrganisationByUuid(ORG_UUID)).thenReturn(Mockito.mock(OrganisationDTO.class));
        when(userService.getUsersByOrganisation(eq(ORG_UUID), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 6L));
        when(userService.getUsersByOrganisationAndDomain(ORG_UUID, "student"))
                .thenReturn(Collections.nCopies(4, (UserDTO) null));
        when(userService.getUsersByOrganisationAndDomain(ORG_UUID, "instructor"))
                .thenReturn(Collections.nCopies(2, (UserDTO) null));
        when(userService.getUsersByOrganisationAndDomain(ORG_UUID, "organisation_user"))
                .thenReturn(Collections.nCopies(1, (UserDTO) null));
        when(trainingBranchService.getTrainingBranchesByOrganisation(eq(ORG_UUID), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 3L));

        mockMvc.perform(get("/api/v1/organisations/{uuid}/statistics", ORG_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organisation_uuid").value(ORG_UUID.toString()))
                .andExpect(jsonPath("$.data.total_members").value(6))
                .andExpect(jsonPath("$.data.total_students").value(4))
                .andExpect(jsonPath("$.data.total_instructors").value(2))
                .andExpect(jsonPath("$.data.total_admins").value(1))
                .andExpect(jsonPath("$.data.total_branches").value(3));
    }

    @Test
    void setOrganisationUserDomainUpsertsMappingAndReturnsUser() throws Exception {
        UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UserDTO updated = new UserDTO(
                userUuid, "000000001", "Jane", null, "Doe", "jane.doe@example.com", "janedoe",
                null, java.time.LocalDate.of(1990, 1, 1), null, true, "kc-123",
                null, null, "system", null, null, List.of("admin"), null);

        when(userService.assignUserToOrganisation(eq(userUuid), eq(ORG_UUID), eq("admin"), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/organisations/{uuid}/users/{userUuid}/domain", ORG_UUID, userUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain_name\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(userUuid.toString()));

        Mockito.verify(userService).assignUserToOrganisation(userUuid, ORG_UUID, "admin", null);
    }

    @Test
    void setOrganisationUserDomainRejectsInvalidDomainWith400() throws Exception {
        UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(put("/api/v1/organisations/{uuid}/users/{userUuid}/domain", ORG_UUID, userUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain_name\":\"course_creator\"}"))
                .andExpect(status().isBadRequest());

        Mockito.verify(userService, Mockito.never())
                .assignUserToOrganisation(any(), any(), any(), any());
    }

    static class MockConfig {
        @Bean
        OrganisationService organisationService() {
            return Mockito.mock(OrganisationService.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        TrainingBranchService trainingBranchService() {
            return Mockito.mock(TrainingBranchService.class);
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
