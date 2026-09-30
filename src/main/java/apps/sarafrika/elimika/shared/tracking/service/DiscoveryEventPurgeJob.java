package apps.sarafrika.elimika.shared.tracking.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly retention for {@code discovery_events}: deletes rows older than 180 days, in bounded
 * batches so no single statement holds locks for long. Safe to run on several instances at once -
 * the delete is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryEventPurgeJob {

    static final int RETENTION_DAYS = 180;
    private static final int BATCH_SIZE = 5_000;
    private static final int MAX_BATCHES = 1_000;

    private final DiscoveryEventService discoveryEventService;

    @Value("${discovery.events.purge.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${discovery.events.purge.cron:0 45 2 * * *}", zone = "UTC")
    public void purgeNightly() {
        if (!enabled) {
            return;
        }
        try {
            long deleted = purgeOlderThan(LocalDateTime.now(ZoneOffset.UTC).minusDays(RETENTION_DAYS));
            log.info("Discovery event retention removed {} row(s) older than {} days", deleted, RETENTION_DAYS);
        } catch (RuntimeException ex) {
            log.error("Discovery event retention failed: {}", ex.getMessage(), ex);
        }
    }

    /** Deletes every event created before {@code cutoff} (UTC); returns the number removed. */
    public long purgeOlderThan(LocalDateTime cutoff) {
        long total = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            int deleted = discoveryEventService.purgeBatch(cutoff, BATCH_SIZE);
            total += deleted;
            if (deleted < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }
}
