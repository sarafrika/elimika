package apps.sarafrika.elimika.perf.repository;

import apps.sarafrika.elimika.perf.model.PerfRumEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PerfRumEventRepository extends JpaRepository<PerfRumEvent, Long> {

    /** Continuous percentiles per route and metric for samples in {@code [from, to)}. */
    @Query(value = """
            SELECT route_template AS "routeTemplate",
                   metric AS "metric",
                   COUNT(*) AS "samples",
                   PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY value_ms) AS "p50",
                   PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY value_ms) AS "p95",
                   PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY value_ms) AS "p99"
            FROM perf_rum_event
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY route_template, metric
            ORDER BY route_template, metric
            """, nativeQuery = true)
    List<PercentileSummary> summarize(@Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("DELETE FROM PerfRumEvent e WHERE e.occurredAt < :cutoff")
    int deleteOccurredBefore(@Param("cutoff") Instant cutoff);

    interface PercentileSummary {
        String getRouteTemplate();

        String getMetric();

        Long getSamples();

        Double getP50();

        Double getP95();

        Double getP99();
    }
}
