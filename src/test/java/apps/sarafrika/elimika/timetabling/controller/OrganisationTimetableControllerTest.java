package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.timetabling.dto.OrganisationTimetableEntryDTO;
import apps.sarafrika.elimika.timetabling.service.OrganisationTimetableService;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganisationTimetableControllerTest {

    @Mock
    private OrganisationTimetableService organisationTimetableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrganisationTimetableController(organisationTimetableService)).build();
    }

    @Test
    void returnsTheOrganisationTimetableForTheDateRange() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        UUID instanceUuid = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 10, 12);
        LocalDate end = LocalDate.of(2026, 10, 18);
        OrganisationTimetableEntryDTO entry = new OrganisationTimetableEntryDTO(
                instanceUuid, UUID.randomUUID(), "Grade 5 Piano", UUID.randomUUID(), "Jane Wanjiku",
                LocalDateTime.of(2026, 10, 12, 9, 0), LocalDateTime.of(2026, 10, 12, 11, 0), "Africa/Nairobi",
                "IN_PERSON", "Room 4", 20, SchedulingStatus.SCHEDULED, 12L);
        when(organisationTimetableService.getOrganisationTimetable(organisationUuid, start, end))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/timetable/organisations/{organisationUuid}", organisationUuid)
                        .param("start", "2026-10-12")
                        .param("end", "2026-10-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value(instanceUuid.toString()))
                .andExpect(jsonPath("$.data[0].class_title").value("Grade 5 Piano"))
                .andExpect(jsonPath("$.data[0].instructor_name").value("Jane Wanjiku"))
                .andExpect(jsonPath("$.data[0].enrolled_count").value(12))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"));

        verify(organisationTimetableService).getOrganisationTimetable(organisationUuid, start, end);
    }

    @Test
    void requiresTheDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/timetable/organisations/{organisationUuid}", UUID.randomUUID())
                        .param("start", "2026-10-12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isReadableOnlyByPlatformAdminsAndOrganisationManagers() throws Exception {
        PreAuthorize rule = OrganisationTimetableController.class
                .getMethod("getOrganisationTimetable", UUID.class, LocalDate.class, LocalDate.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(rule.value()).isEqualTo(
                "@domainSecurityService.isPlatformAdmin() or @domainSecurityService.managesOrganisation(#organisationUuid)");
    }
}
