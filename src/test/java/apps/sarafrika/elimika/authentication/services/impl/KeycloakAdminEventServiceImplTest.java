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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    void getAdminEventSummaryAggregatesAcrossPagesAndWindows() {
        List<AdminEventRepresentation> pageOne = List.of(event("CREATE", "USER"), event("UPDATE", "COURSE"));
        List<AdminEventRepresentation> pageTwo = List.of(event("CREATE", "USER"));
        List<AdminEventRepresentation> sevenDayWindow = List.of(event("DELETE", "REALM"), event("UPDATE", "REALM"));

        when(keycloak.realm("elimika")).thenReturn(realmResource);
        when(realmResource.getAdminEvents(any(), anyString(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyInt(), anyInt(), any()))
                .thenReturn(pageOne)
                .thenReturn(pageTwo)
                .thenReturn(sevenDayWindow)
                .thenReturn(Collections.emptyList());

        KeycloakAdminEventSummary summary = service.getAdminEventSummary();

        assertThat(summary.eventsLast24Hours()).isEqualTo(3);
        assertThat(summary.eventsLast7Days()).isEqualTo(2);
        assertThat(summary.operationsLast24Hours())
                .containsEntry("CREATE", 2L)
                .containsEntry("UPDATE", 1L);
        assertThat(summary.resourceTypesLast24Hours())
                .containsEntry("USER", 2L)
                .containsEntry("COURSE", 1L);
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

    private AdminEventRepresentation event(String operation, String resourceType) {
        AdminEventRepresentation representation = new AdminEventRepresentation();
        representation.setOperationType(operation);
        representation.setResourceType(resourceType);
        return representation;
    }
}
