package apps.sarafrika.elimika.perf.service;

import apps.sarafrika.elimika.perf.dto.RumEventRequest;
import apps.sarafrika.elimika.perf.dto.RumIngestResponse;
import apps.sarafrika.elimika.perf.dto.RumSummaryResponse;
import apps.sarafrika.elimika.perf.dto.RumSummaryRow;
import apps.sarafrika.elimika.perf.model.PerfRumEvent;
import apps.sarafrika.elimika.perf.repository.PerfRumEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PerfRumService {

    public static final Duration RETENTION = Duration.ofDays(30);
    static final Duration FUTURE_SKEW = Duration.ofMinutes(5);
    static final Duration DEFAULT_SUMMARY_WINDOW = Duration.ofDays(1);

    private final PerfRumEventRepository repository;
    private final Clock clock;

    @Autowired
    public PerfRumService(PerfRumEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    PerfRumService(PerfRumEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Stores the samples whose timestamp falls inside retention; the rest are counted as dropped. */
    @Transactional
    public RumIngestResponse ingest(List<RumEventRequest> events, boolean authenticated) {
        Instant now = clock.instant();
        Instant oldest = now.minus(RETENTION);
        Instant newest = now.plus(FUTURE_SKEW);
        List<PerfRumEvent> rows = new ArrayList<>(events.size());
        for (RumEventRequest event : events) {
            Instant occurredAt = event.occurredAt().toInstant();
            if (occurredAt.isBefore(oldest) || occurredAt.isAfter(newest)) {
                continue;
            }
            rows.add(toEntity(event, occurredAt, authenticated, now));
        }
        repository.saveAll(rows);
        return new RumIngestResponse(rows.size(), events.size() - rows.size());
    }

    @Transactional(readOnly = true)
    public RumSummaryResponse summarize(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime end = to != null ? to : OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime start = from != null ? from : end.minus(DEFAULT_SUMMARY_WINDOW);
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        List<RumSummaryRow> rows = repository.summarize(start.toInstant(), end.toInstant()).stream()
                .map(row -> new RumSummaryRow(row.getRouteTemplate(), row.getMetric(), row.getSamples(),
                        row.getP50(), row.getP95(), row.getP99()))
                .toList();
        return new RumSummaryResponse(start, end, rows);
    }

    @Transactional
    public int purgeExpired() {
        int deleted = repository.deleteOccurredBefore(clock.instant().minus(RETENTION));
        if (deleted > 0) {
            log.info("Purged {} real-user performance samples older than {}", deleted, RETENTION);
        }
        return deleted;
    }

    private static PerfRumEvent toEntity(RumEventRequest event, Instant occurredAt, boolean authenticated,
                                         Instant receivedAt) {
        PerfRumEvent entity = new PerfRumEvent();
        entity.setRouteTemplate(event.routeTemplate().trim());
        entity.setDomain(trimToNull(event.domain()));
        entity.setMetric(event.metric().trim());
        entity.setValueMs(event.valueMs());
        entity.setSection(trimToNull(event.section()));
        entity.setNetworkType(trimToNull(event.networkType()));
        entity.setDeviceClass(trimToNull(event.deviceClass()));
        entity.setAppVersion(trimToNull(event.appVersion()));
        entity.setAuthenticated(authenticated);
        entity.setOccurredAt(occurredAt);
        entity.setReceivedAt(receivedAt);
        return entity;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
