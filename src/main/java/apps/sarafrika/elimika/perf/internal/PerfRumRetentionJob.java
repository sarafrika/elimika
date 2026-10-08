package apps.sarafrika.elimika.perf.internal;

import apps.sarafrika.elimika.perf.service.PerfRumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes real-user samples past the 30-day retention window once a day. */
@Component
@RequiredArgsConstructor
@Slf4j
class PerfRumRetentionJob {

    private final PerfRumService perfRumService;

    @Scheduled(cron = "${perf.rum.retention-cron:0 17 3 * * *}", zone = "UTC")
    void purgeExpiredSamples() {
        try {
            perfRumService.purgeExpired();
        } catch (Exception ex) {
            log.error("Failed to purge expired real-user performance samples: {}", ex.getMessage(), ex);
        }
    }
}
