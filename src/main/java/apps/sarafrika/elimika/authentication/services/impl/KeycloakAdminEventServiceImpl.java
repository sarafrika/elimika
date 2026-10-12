package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventSummary;
import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminEventServiceImpl implements KeycloakAdminEventService {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final Keycloak keycloak;

    @Value("${app.keycloak.realm}")
    private String realm;

    @Value("${app.keycloak.admin.events.page-size:" + DEFAULT_PAGE_SIZE + "}")
    private int pageSize;

    @Value("${app.keycloak.admin.events.direction:desc}")
    private String direction;

    @Value("${app.keycloak.admin.events.auth-client:}")
    private String adminEventsClientId;

    @Override
    public KeycloakAdminEventSummary getAdminEventSummary() {
        Instant now = Instant.now();
        // One 7-day read; the 24-hour window is the slice of it at or after the cutoff.
        AdminEventWindowStats last7Days = fetchAdminEvents(now.minus(7, ChronoUnit.DAYS), now,
                now.minus(24, ChronoUnit.HOURS).toEpochMilli());

        return new KeycloakAdminEventSummary(
                last7Days.recentEvents(),
                last7Days.totalEvents(),
                last7Days.eventsByOperation(),
                last7Days.eventsByResourceType()
        );
    }

    private AdminEventWindowStats fetchAdminEvents(Instant from, Instant to, long recentFromMillis) {
        if (!StringUtils.hasText(realm)) {
            log.warn("Keycloak realm is not configured; admin event summary will be empty");
            return AdminEventWindowStats.empty();
        }

        int resolvedPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        long total = 0;
        long recent = 0;
        Map<String, Long> byOperation = new HashMap<>();
        Map<String, Long> byResourceType = new HashMap<>();

        try {
            RealmResource realmResource = keycloak.realm(realm);
            int first = 0;
            while (true) {
                List<AdminEventRepresentation> batch = realmResource.getAdminEvents(
                        null,
                        realm,
                        resolveAuthClient(),
                        null,
                        null,
                        null,
                        null,
                        from.toEpochMilli(),
                        to.toEpochMilli(),
                        first,
                        resolvedPageSize,
                        direction.toLowerCase(Locale.ROOT)
                );

                if (batch == null || batch.isEmpty()) {
                    break;
                }

                for (AdminEventRepresentation event : batch) {
                    total++;
                    if (event.getTime() >= recentFromMillis) {
                        recent++;
                        updateCounter(byOperation, event.getOperationType());
                        updateCounter(byResourceType, event.getResourceType());
                    }
                }

                if (batch.size() < resolvedPageSize) {
                    break;
                }
                first += batch.size();
            }

            return new AdminEventWindowStats(total, recent, byOperation, byResourceType);
        } catch (Exception ex) {
            log.warn("Failed to fetch Keycloak admin events for range {} - {}: {}", from, to, ex.getMessage());
            log.debug("Keycloak admin events retrieval error", ex);
            return AdminEventWindowStats.empty();
        }
    }

    private String resolveAuthClient() {
        return StringUtils.hasText(adminEventsClientId) ? adminEventsClientId : null;
    }

    private void updateCounter(Map<String, Long> target, String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        target.merge(key, 1L, Long::sum);
    }

    private record AdminEventWindowStats(long totalEvents,
                                         long recentEvents,
                                         Map<String, Long> eventsByOperation,
                                         Map<String, Long> eventsByResourceType) {
        static AdminEventWindowStats empty() {
            return new AdminEventWindowStats(0, 0, Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
