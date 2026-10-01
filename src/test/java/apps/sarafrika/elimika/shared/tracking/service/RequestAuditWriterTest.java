package apps.sarafrika.elimika.shared.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.tracking.model.RequestAuditEntry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

class RequestAuditWriterTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void writesQueuedEntriesInBatchesOfConfiguredSize() {
        RequestAuditWriter writer = new RequestAuditWriter(jdbcTemplate, meterRegistry, 2_000, 500, 1_000);
        for (int i = 0; i < 1_200; i++) {
            assertThat(writer.offer(entry(i))).isTrue();
        }

        writer.flushAll();

        assertThat(capturedBatchSizes()).containsExactly(500, 500, 200);
        assertThat(writer.pendingCount()).isZero();
        assertThat(meterRegistry.counter("elimika.audit.written").count()).isEqualTo(1_200);
    }

    @Test
    void dropsAndCountsEntriesWhenQueueIsFull() {
        RequestAuditWriter writer = new RequestAuditWriter(jdbcTemplate, meterRegistry, 3, 500, 1_000);

        assertThat(writer.offer(entry(1))).isTrue();
        assertThat(writer.offer(entry(2))).isTrue();
        assertThat(writer.offer(entry(3))).isTrue();
        assertThat(writer.offer(entry(4))).isFalse();
        assertThat(writer.offer(entry(5))).isFalse();

        assertThat(meterRegistry.counter("elimika.audit.dropped").count()).isEqualTo(2);
        assertThat(writer.pendingCount()).isEqualTo(3);
        verify(jdbcTemplate, never()).batchUpdate(any(String.class), any(BatchPreparedStatementSetter.class));
    }

    @Test
    void countsFailedBatchWithoutPropagating() {
        when(jdbcTemplate.batchUpdate(eq(RequestAuditWriter.INSERT_SQL), any(BatchPreparedStatementSetter.class)))
                .thenThrow(new IllegalStateException("db down"));
        RequestAuditWriter writer = new RequestAuditWriter(jdbcTemplate, meterRegistry, 10, 500, 1_000);
        writer.offer(entry(1));
        writer.offer(entry(2));

        writer.flushAll();

        assertThat(meterRegistry.counter("elimika.audit.failed").count()).isEqualTo(2);
        assertThat(writer.pendingCount()).isZero();
    }

    @Test
    void stopFlushesEverythingStillQueued() {
        RequestAuditWriter writer = new RequestAuditWriter(jdbcTemplate, meterRegistry, 100, 500, 60_000);
        writer.start();
        for (int i = 0; i < 7; i++) {
            writer.offer(entry(i));
        }

        writer.stop();

        assertThat(writer.isRunning()).isFalse();
        assertThat(writer.pendingCount()).isZero();
        assertThat(capturedBatchSizes().stream().mapToInt(Integer::intValue).sum()).isEqualTo(7);
    }

    private List<Integer> capturedBatchSizes() {
        ArgumentCaptor<BatchPreparedStatementSetter> captor =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate, atLeastOnce()).batchUpdate(eq(RequestAuditWriter.INSERT_SQL), captor.capture());
        return captor.getAllValues().stream().map(BatchPreparedStatementSetter::getBatchSize).toList();
    }

    private static RequestAuditEntry entry(int i) {
        return new RequestAuditEntry("req-" + i, "GET", "/api/v1/courses", null, "127.0.0.1", "localhost",
                "k6", null, null, "{}", 200, 3L, null, null, null, null, null, LocalDateTime.now(), "SYSTEM");
    }
}
