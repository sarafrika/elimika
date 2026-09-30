package apps.sarafrika.elimika.course.internal.recommend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Nightly refresh of the course feature tables, at 01:00 UTC by default: half an hour before the full
 * search rebuild ({@code search.full-rebuild-cron}, 01:30 UTC) copies the non-personal aggregates into the
 * {@code courses} index. Set {@code course.features.refresh-cron} to {@code -} to switch it off.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseFeatureRefreshJob {

    private final CourseFeatureRefresher refresher;

    @Scheduled(cron = "${course.features.refresh-cron:0 0 1 * * *}", zone = "UTC")
    public void refresh() {
        try {
            refresher.refresh(Instant.now());
        } catch (Exception ex) {
            // A failed run leaves last night's tables in place (the rewrite is one transaction); the
            // scheduler must survive it and try again tomorrow.
            log.error("Course feature refresh failed: {}", ex.getMessage(), ex);
        }
    }
}
