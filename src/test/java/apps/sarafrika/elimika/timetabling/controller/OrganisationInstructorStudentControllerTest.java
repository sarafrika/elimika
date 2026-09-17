package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import apps.sarafrika.elimika.timetabling.dto.InstructorClassOptionDTO;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentDTO;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService.InstructorStudentRoster;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrganisationInstructorStudentController.class, properties = "app.keycloak.realm=test-realm")
@ExtendWith(SpringExtension.class)
@Import({OrganisationInstructorStudentControllerTest.MockConfig.class,
        OrganisationInstructorStudentControllerTest.MethodSecurityConfig.class})
class OrganisationInstructorStudentControllerTest {

    private static final UUID ORGANISATION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTRUCTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PATH = "/api/v1/organisations/{organisationUuid}/instructors/{instructorUuid}/students";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstructorStudentRosterService rosterService;

    @Autowired
    private DomainSecurityService domainSecurityService;

    @BeforeEach
    void setUp() {
        reset(rosterService, domainSecurityService);
    }

    @Test
    void aCallerWhoDoesNotManageTheOrganisationIsForbidden() throws Exception {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(false);

        mockMvc.perform(get(PATH, ORGANISATION, INSTRUCTOR).with(jwt()))
                .andExpect(status().isForbidden());

        verify(rosterService, never()).listInstructorStudents(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void aManagerGetsThePageWithItsClassOptions() throws Exception {
        UUID student = UUID.randomUUID();
        UUID classUuid = UUID.randomUUID();
        when(domainSecurityService.managesOrganisation(ORGANISATION)).thenReturn(true);
        when(rosterService.listInstructorStudents(ORGANISATION, INSTRUCTOR, "ami", classUuid, 1, 5))
                .thenReturn(new InstructorStudentRoster(
                        new PageImpl<>(List.of(new InstructorStudentDTO(student, "Amina Otieno", classUuid,
                                "Grade 5 Piano", "Beginner Piano", SessionFormat.GROUP, LocationType.IN_PERSON,
                                "Mon & Wed · 9:00–11:00", null, null, LocalDateTime.of(2031, 4, 1, 8, 0), 83.3,
                                EnrollmentStatus.ENROLLED)), PageRequest.of(1, 5), 6),
                        List.of(new InstructorClassOptionDTO(classUuid, "Grade 5 Piano"))));

        mockMvc.perform(get(PATH, ORGANISATION, INSTRUCTOR).with(jwt())
                        .param("search", "ami")
                        .param("class_definition_uuid", classUuid.toString())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].student_uuid").value(student.toString()))
                .andExpect(jsonPath("$.data.content[0].student_name").value("Amina Otieno"))
                .andExpect(jsonPath("$.data.content[0].class_definition_uuid").value(classUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].class_title").value("Grade 5 Piano"))
                .andExpect(jsonPath("$.data.content[0].course_name").value("Beginner Piano"))
                .andExpect(jsonPath("$.data.content[0].session_format").value("GROUP"))
                .andExpect(jsonPath("$.data.content[0].location_type").value("IN_PERSON"))
                .andExpect(jsonPath("$.data.content[0].schedule_summary").value("Mon & Wed · 9:00–11:00"))
                .andExpect(jsonPath("$.data.content[0].enrolled_at").value("2031-04-01T08:00:00"))
                .andExpect(jsonPath("$.data.content[0].attendance_rate").value(83.3))
                .andExpect(jsonPath("$.data.content[0].enrollment_status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(6))
                .andExpect(jsonPath("$.data.class_options[0].class_definition_uuid").value(classUuid.toString()))
                .andExpect(jsonPath("$.data.class_options[0].class_title").value("Grade 5 Piano"));
    }

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
        InstructorStudentRosterService rosterService() {
            return Mockito.mock(InstructorStudentRosterService.class);
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
