package apps.sarafrika.elimika.search.internal.meilisearch;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reports the engine as the {@code search} health component. Only registered when search is enabled,
 * so a deployment without Meilisearch never reports DOWN because of it.
 */
@Component("searchHealthIndicator")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class MeilisearchHealthIndicator implements HealthIndicator {

    private final MeilisearchGateway gateway;

    @Override
    public Health health() {
        return gateway.isAvailable()
                ? Health.up().withDetail("engine", "meilisearch").build()
                : Health.down().withDetail("engine", "meilisearch").build();
    }
}
