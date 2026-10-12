package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminEventServiceImplTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    private KeycloakAdminEventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminEventServiceImpl(keycloak);
        ReflectionTestUtils.setField(service, "realm", "elimika");
        ReflectionTestUtils.setField(service, "pageSize", 2);
        ReflectionTestUtils.setField(service, "direction", "DESC");
        ReflectionTestUtils.setField(service, "adminEventsClientId", "");
    }

    @Test
    void getAdminEventSummaryReadsSevenDaysOnceAndSlicesTheLastDay() {
        long now = System.currentTimeMillis();
        long recent = now - Duration.ofHours(1).toMillis();
        long older = now - Duration.ofDays(3).toMillis();
        List<AdminEventRepresentation> pageOne = List.of(event("CREATE", "USER", recent), event("UPDATE", "COURSE", recent));
        List<AdminEventRepresentation> pageTwo = List.of(event("CREATE", "USER", recent), event("DELETE", "REALM", older));
        List<AdminEventRepresentation> pageThree = List.of(event("UPDATE", "REALM", older));

        when(keycloak.realm("elimika")).thenReturn(realmResource);
        when(realmResource.getAdminEvents(any(), anyString(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(pageOne)
                .thenReturn(pageTwo)
                .thenReturn(pageThree);

        KeycloakAdminEventSummary summary = service.getAdminEventSummary();

        assertThat(summary.eventsLast24Hours()).isEqualTo(3);
        assertThat(summary.eventsLast7Days()).isEqualTo(5);
        assertThat(summary.operationsLast24Hours())
                .containsOnly(Map.entry("CREATE", 2L), Map.entry("UPDATE", 1L));
        assertThat(summary.resourceTypesLast24Hours())
                .containsOnly(Map.entry("USER", 2L), Map.entry("COURSE", 1L));
        // Three pages of the single 7-day window: no second window is fetched.
        verify(realmResource, times(3)).getAdminEvents(any(), anyString(), any(), any(), any(), any(), any(),
                anyLong(), anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void getAdminEventSummaryReturnsEmptySnapshotWhenKeycloakUnavailable() {
        when(keycloak.realm("elimika")).thenThrow(new RuntimeException("Keycloak offline"));

        KeycloakAdminEventSummary summary = service.getAdminEventSummary();

        assertThat(summary.eventsLast24Hours()).isZero();
        assertThat(summary.eventsLast7Days()).isZero();
        assertThat(summary.operationsLast24Hours()).isEmpty();
        assertThat(summary.resourceTypesLast24Hours()).isEmpty();
    }

    private AdminEventRepresentation event(String operation, String resourceType, long time) {
        AdminEventRepresentation representation = new AdminEventRepresentation();
        representation.setTime(time);
        representation.setOperationType(operation);
        representation.setResourceType(resourceType);
        return representation;
    }
}
