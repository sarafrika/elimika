package apps.sarafrika.elimika.authentication.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Snapshot of Keycloak admin event activity for recent windows.
 */
public record KeycloakAdminEventSummary(

        @JsonProperty("events_last_24h")
        long eventsLast24Hours,

        @JsonProperty("events_last_7d")
        long eventsLast7Days,

        @JsonProperty("operations_last_24h")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Long> operationsLast24Hours,

        @JsonProperty("resource_types_last_24h")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Long> resourceTypesLast24Hours
) {

    public KeycloakAdminEventSummary {
        operationsLast24Hours = operationsLast24Hours == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(operationsLast24Hours);
        resourceTypesLast24Hours = resourceTypesLast24Hours == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(resourceTypesLast24Hours);
    }
}
