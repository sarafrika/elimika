package apps.sarafrika.elimika.perf.service;

import apps.sarafrika.elimika.perf.dto.RumEventRequest;
import apps.sarafrika.elimika.perf.dto.RumIngestResponse;
import apps.sarafrika.elimika.perf.dto.RumSummaryResponse;
import apps.sarafrika.elimika.perf.model.PerfRumEvent;
import apps.sarafrika.elimika.perf.repository.PerfRumEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerfRumServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");

    private final PerfRumEventRepository repository = mock(PerfRumEventRepository.class);
    private final PerfRumService service = new PerfRumService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @SuppressWarnings("unchecked")
    void storesSamplesInsideTheWindowAndDropsTheRest() {
        RumEventRequest fresh = event(NOW.minusSeconds(30));
        RumEventRequest expired = event(NOW.minus(Duration.ofDays(31)));
        RumEventRequest future = event(NOW.plus(Duration.ofHours(1)));

        RumIngestResponse response = service.ingest(List.of(fresh, expired, future), true);

        assertThat(response.accepted()).isEqualTo(1);
        assertThat(response.dropped()).isEqualTo(2);
        ArgumentCaptor<List<PerfRumEvent>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        PerfRumEvent row = saved.getValue().getFirst();
        assertThat(row.getRouteTemplate()).isEqualTo("/dashboard/overview");
        assertThat(row.getSection()).isNull();
        assertThat(row.isAuthenticated()).isTrue();
        assertThat(row.getOccurredAt()).isEqualTo(NOW.minusSeconds(30));
        assertThat(row.getReceivedAt()).isEqualTo(NOW);
    }

    @Test
    void summaryDefaultsToTheLastDay() {
        when(repository.summarize(any(), any())).thenReturn(List.of());

        RumSummaryResponse response = service.summarize(null, null);

        assertThat(response.to().toInstant()).isEqualTo(NOW);
        assertThat(response.from().toInstant()).isEqualTo(NOW.minus(Duration.ofDays(1)));
        verify(repository).summarize(eq(NOW.minus(Duration.ofDays(1))), eq(NOW));
    }

    @Test
    void summaryRejectsAnInvertedWindow() {
        OffsetDateTime to = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
        assertThatThrownBy(() -> service.summarize(to, to.minusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void purgeDeletesRowsOlderThanThirtyDays() {
        service.purgeExpired();
        verify(repository).deleteOccurredBefore(NOW.minus(Duration.ofDays(30)));
    }

    private static RumEventRequest event(Instant occurredAt) {
        return new RumEventRequest(" /dashboard/overview ", "student", "LCP", 1200.0, "  ", "4g", "mobile",
                "1.0.0", OffsetDateTime.ofInstant(occurredAt, ZoneOffset.UTC));
    }
}
