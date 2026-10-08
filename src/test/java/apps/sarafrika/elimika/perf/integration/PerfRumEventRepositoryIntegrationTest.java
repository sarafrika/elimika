package apps.sarafrika.elimika.perf.integration;

import apps.sarafrika.elimika.perf.model.PerfRumEvent;
import apps.sarafrika.elimika.perf.repository.PerfRumEventRepository;
import apps.sarafrika.elimika.perf.repository.PerfRumEventRepository.PercentileSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Real-user samples summarise into percentiles and expire after retention")
class PerfRumEventRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");

    @Autowired
    private PerfRumEventRepository repository;

    @Test
    @DisplayName("percentiles are grouped per route and metric and bounded by [from, to)")
    void summarisesPercentilesPerRouteAndMetric() {
        IntStream.rangeClosed(1, 100).forEach(i -> repository.save(sample("/dashboard/overview", "LCP", i * 10.0,
                NOW.minusSeconds(i))));
        repository.save(sample("/dashboard/overview", "TTFB", 50.0, NOW.minusSeconds(5)));
        repository.save(sample("/dashboard/overview", "LCP", 99_999.0, NOW.minus(Duration.ofDays(2))));
        repository.flush();

        List<PercentileSummary> rows = repository.summarize(NOW.minus(Duration.ofDays(1)), NOW);

        assertThat(rows).hasSize(2);
        PercentileSummary lcp = rows.getFirst();
        assertThat(lcp.getMetric()).isEqualTo("LCP");
        assertThat(lcp.getRouteTemplate()).isEqualTo("/dashboard/overview");
        assertThat(lcp.getSamples()).isEqualTo(100L);
        assertThat(lcp.getP50()).isCloseTo(505.0, within(0.01));
        assertThat(lcp.getP95()).isCloseTo(950.5, within(0.01));
        assertThat(lcp.getP99()).isCloseTo(990.1, within(0.01));
        assertThat(rows.get(1).getMetric()).isEqualTo("TTFB");
    }

    @Test
    @DisplayName("the retention delete removes only rows older than the cutoff")
    void deletesOnlyExpiredRows() {
        repository.save(sample("/a", "LCP", 1.0, NOW.minus(Duration.ofDays(31))));
        repository.save(sample("/a", "LCP", 1.0, NOW.minus(Duration.ofDays(29))));
        repository.flush();

        int deleted = repository.deleteOccurredBefore(NOW.minus(Duration.ofDays(30)));

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    private static PerfRumEvent sample(String route, String metric, double valueMs, Instant occurredAt) {
        PerfRumEvent event = new PerfRumEvent();
        event.setRouteTemplate(route);
        event.setMetric(metric);
        event.setValueMs(valueMs);
        event.setOccurredAt(occurredAt);
        event.setReceivedAt(NOW);
        return event;
    }
}
