package apps.sarafrika.elimika.availability.controller;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleLookupService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
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

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AvailabilityController.class, properties = "app.keycloak.realm=test-realm")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@Import(AvailabilityControllerTest.MockConfig.class)
class AvailabilityControllerTest {

    private static final UUID INSTRUCTOR_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        reset(availabilityService);
    }

    private AvailabilitySlotDTO sampleSlot(UUID uuid, UUID instructorUuid) {
        return new AvailabilitySlotDTO(
                uuid,
                instructorUuid,
                AvailabilityType.WEEKLY,
                1,
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                null,
                Boolean.TRUE,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void createAvailabilitySlotReturnsCreated() throws Exception {
        AvailabilitySlotDTO request = sampleSlot(null, INSTRUCTOR_UUID);
        AvailabilitySlotDTO response = sampleSlot(UUID.randomUUID(), INSTRUCTOR_UUID);

        when(availabilityService.createAvailabilitySlot(any(AvailabilitySlotDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/instructors/{instructorUuid}/availability/slots", INSTRUCTOR_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.instructor_uuid").value(INSTRUCTOR_UUID.toString()));

        ArgumentCaptor<AvailabilitySlotDTO> captor = ArgumentCaptor.forClass(AvailabilitySlotDTO.class);
        verify(availabilityService).createAvailabilitySlot(captor.capture());
        assertEquals(INSTRUCTOR_UUID, captor.getValue().instructorUuid());
    }

    @Test
    void listAvailabilitySlotsReturnsOk() throws Exception {
        when(availabilityService.getAvailabilityForInstructor(INSTRUCTOR_UUID))
                .thenReturn(List.of(sampleSlot(UUID.randomUUID(), INSTRUCTOR_UUID)));

        mockMvc.perform(get("/api/v1/instructors/{instructorUuid}/availability/slots", INSTRUCTOR_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].instructor_uuid").value(INSTRUCTOR_UUID.toString()));

        verify(availabilityService).getAvailabilityForInstructor(INSTRUCTOR_UUID);
    }

    static class MockConfig {
        @Bean
        AvailabilityService availabilityService() {
            return Mockito.mock(AvailabilityService.class);
        }

        @Bean
        InstructorScheduleLookupService instructorScheduleLookupService() {
            return Mockito.mock(InstructorScheduleLookupService.class);
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
