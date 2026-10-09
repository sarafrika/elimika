package apps.sarafrika.elimika.shared.storage.internal;

import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clears catalogue media columns (course, programme, class thumbnails and banners) whose stored
 * file no longer exists, so the catalogue stops advertising URLs that 404 on every page view.
 * Runs once at startup; idempotent, and only ever writes NULL over a reference to a missing file.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.media", name = "sweep-dangling-refs", havingValue = "true")
public class MediaReferenceSweep {

    static final int BATCH_SIZE = 500;

    /** Index names mirror the course module's search sources; shared cannot import them. */
    private static final String COURSES_INDEX = "courses";
    private static final String PROGRAMS_INDEX = "programs";

    private static final List<MediaColumn> COLUMNS = List.of(
            new MediaColumn("courses", "thumbnail_url"),
            new MediaColumn("courses", "banner_url"),
            new MediaColumn("courses", "intro_video_url"),
            new MediaColumn("training_programs", "thumbnail_url"),
            new MediaColumn("training_programs", "banner_url"),
            new MediaColumn("class_definitions", "thumbnail_url"));

    record MediaColumn(String table, String column) {
    }

    record DanglingRef(MediaColumn column, UUID rowUuid, String value) {
    }

    private final StorageService storageService;
    private final JdbcTemplate jdbcTemplate;
    private final SearchIndexRequests searchIndexRequests;
    private final TransactionTemplate transactionTemplate;
    private final double maxMissingRatio;

    public MediaReferenceSweep(
            StorageService storageService,
            JdbcTemplate jdbcTemplate,
            SearchIndexRequests searchIndexRequests,
            PlatformTransactionManager transactionManager,
            @Value("${app.media.sweep-max-missing-ratio:0.5}") double maxMissingRatio
    ) {
        this.storageService = storageService;
        this.jdbcTemplate = jdbcTemplate;
        this.searchIndexRequests = searchIndexRequests;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxMissingRatio = maxMissingRatio;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            sweep();
        } catch (Exception ex) {
            log.warn("Dangling media reference sweep failed: {}", ex.getMessage(), ex);
        }
    }

    /** @return the number of references cleared */
    public int sweep() {
        Map<String, Boolean> existence = new HashMap<>();
        List<DanglingRef> dangling = new ArrayList<>();
        int checked = 0;
        for (MediaColumn column : COLUMNS) {
            List<DanglingRef> rows = new ArrayList<>();
            jdbcTemplate.query(
                    "SELECT uuid, " + column.column() + " FROM " + column.table()
                            + " WHERE " + column.column() + " IS NOT NULL",
                    rs -> {
                        rows.add(new DanglingRef(column, UUID.fromString(rs.getString(1)), rs.getString(2)));
                    });
            for (DanglingRef row : rows) {
                String key = FileUrlResolver.toKey(row.value());
                if (key == null || key.isBlank()) {
                    continue;
                }
                checked++;
                if (!existence.computeIfAbsent(key, storageService::exists)) {
                    dangling.add(row);
                }
            }
        }

        // An unmounted or empty volume makes every file look missing; never wipe on that signal.
        if (checked > 0 && (dangling.size() == checked || (double) dangling.size() / checked > maxMissingRatio)) {
            log.warn("Dangling media sweep skipped: {} of {} references look missing, above the {} safety ratio",
                    dangling.size(), checked, maxMissingRatio);
            return 0;
        }

        int cleared = 0;
        for (int from = 0; from < dangling.size(); from += BATCH_SIZE) {
            List<DanglingRef> batch = dangling.subList(from, Math.min(from + BATCH_SIZE, dangling.size()));
            Integer updated = transactionTemplate.execute(status -> clear(batch));
            cleared += updated == null ? 0 : updated;
        }
        log.info("Dangling media sweep: {} references checked, {} cleared", checked, cleared);
        return cleared;
    }

    private int clear(List<DanglingRef> batch) {
        int cleared = 0;
        for (DanglingRef ref : batch) {
            // Matching the old value keeps a concurrent re-upload from being wiped.
            int rows = jdbcTemplate.update(
                    "UPDATE " + ref.column().table() + " SET " + ref.column().column() + " = NULL"
                            + " WHERE uuid = ? AND " + ref.column().column() + " = ?",
                    ref.rowUuid(), ref.value());
            if (rows > 0) {
                cleared += rows;
                reindex(ref);
            }
        }
        return cleared;
    }

    /** JDBC writes bypass the entity listener, so the search documents are re-synced by hand. */
    private void reindex(DanglingRef ref) {
        switch (ref.column().table()) {
            case "courses" -> {
                searchIndexRequests.enqueue(COURSES_INDEX, ref.rowUuid());
                searchIndexRequests.enqueueFanOut(PROGRAMS_INDEX, "course:" + ref.rowUuid());
            }
            case "training_programs" -> searchIndexRequests.enqueue(PROGRAMS_INDEX, ref.rowUuid());
            default -> {
                // Class thumbnails are not part of any search document.
            }
        }
    }
}
