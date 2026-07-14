package apps.sarafrika.elimika.shared.storage.internal;

import apps.sarafrika.elimika.shared.storage.model.MediaFile;
import apps.sarafrika.elimika.shared.storage.repository.MediaFileRepository;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Cross-checks the storage directory, the {@code media_files} registry and every
 * domain column holding a file reference.
 * <p>
 * {@code reconcile} fills missing registry metadata and flags registry rows whose
 * file is gone; with pruning enabled it also NULLs domain references to lost files
 * so API responses stop advertising URLs that would 404 (clients then show a
 * placeholder and users can re-upload).
 * <p>
 * {@code sweep} reports files on disk that nothing references (orphans) and can
 * delete them when explicitly confirmed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaReconciliationService {

    /**
     * Every domain table.column that persists a file reference. The backfill
     * migrations normalize these to bare storage keys; external URLs are skipped
     * at read time via {@link FileUrlResolver#toKey}.
     */
    private static final List<MediaColumn> MEDIA_COLUMNS = List.of(
            new MediaColumn("users", "profile_image_url"),
            new MediaColumn("courses", "thumbnail_url"),
            new MediaColumn("courses", "banner_url"),
            new MediaColumn("courses", "intro_video_url"),
            new MediaColumn("lesson_contents", "file_url"),
            new MediaColumn("class_definitions", "thumbnail_url"),
            new MediaColumn("class_definitions", "promotional_video_url"),
            new MediaColumn("certificates", "certificate_url"),
            new MediaColumn("certificate_templates", "background_image_url"),
            new MediaColumn("assignment_attachments", "file_url"),
            new MediaColumn("assignment_submission_attachments", "file_url"),
            new MediaColumn("class_resources", "file_path"),
            new MediaColumn("instructor_documents", "file_path"),
            new MediaColumn("course_creator_documents", "file_path")
    );

    private record MediaColumn(String table, String column) {
    }

    public record DeadReference(String table, String column, UUID rowUuid, String value) {
    }

    public record ReconcileReport(
            int registryRowsChecked,
            int registryMarkedMissing,
            int registryMetadataFilled,
            List<DeadReference> deadReferences,
            int deadReferencesPruned
    ) {
    }

    public record SweepReport(
            int diskFiles,
            int referencedKeys,
            List<String> orphanKeys,
            int orphansDeleted
    ) {
    }

    private final StorageService storageService;
    private final MediaFileRepository mediaFileRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Reconciles registry and domain references against the files actually on disk.
     *
     * @param pruneDeadReferences when true, domain columns referencing lost files are
     *                            set to NULL (array columns have dead elements removed)
     */
    @Transactional
    public ReconcileReport reconcile(boolean pruneDeadReferences) {
        Set<String> diskKeys = new HashSet<>(storageService.listAllKeys());

        int markedMissing = 0;
        int metadataFilled = 0;
        List<MediaFile> registryRows = mediaFileRepository.findAll();
        for (MediaFile mediaFile : registryRows) {
            boolean exists = diskKeys.contains(mediaFile.getFileKey());
            boolean dirty = false;
            if (mediaFile.isFileExists() != exists) {
                mediaFile.setFileExists(exists);
                dirty = true;
                if (!exists) {
                    markedMissing++;
                }
            }
            if (exists && (mediaFile.getSizeBytes() == null || mediaFile.getMimeType() == null)) {
                fillMetadata(mediaFile);
                metadataFilled++;
                dirty = true;
            }
            if (dirty) {
                mediaFileRepository.save(mediaFile);
            }
        }

        List<DeadReference> deadReferences = findDeadReferences(diskKeys);
        int pruned = 0;
        if (pruneDeadReferences) {
            for (DeadReference dead : deadReferences) {
                jdbcTemplate.update(
                        "UPDATE " + dead.table() + " SET " + dead.column() + " = NULL WHERE uuid = ?",
                        dead.rowUuid());
                pruned++;
            }
            pruned += pruneDeadSubmissionFileUrls(diskKeys);
        }

        log.info("Media reconciliation: {} registry rows, {} marked missing, {} metadata filled, {} dead refs ({} pruned)",
                registryRows.size(), markedMissing, metadataFilled, deadReferences.size(), pruned);
        return new ReconcileReport(registryRows.size(), markedMissing, metadataFilled, deadReferences, pruned);
    }

    /**
     * Reports files on disk that neither the registry nor any domain column references.
     *
     * @param deleteOrphans when true, orphaned files are removed from disk
     */
    @Transactional(readOnly = true)
    public SweepReport sweep(boolean deleteOrphans) {
        List<String> diskKeys = storageService.listAllKeys();
        Set<String> referenced = collectReferencedKeys();
        mediaFileRepository.findAll().forEach(mediaFile -> referenced.add(mediaFile.getFileKey()));

        List<String> orphans = diskKeys.stream()
                .filter(key -> !referenced.contains(key))
                .sorted()
                .toList();

        int deleted = 0;
        if (deleteOrphans) {
            for (String orphan : orphans) {
                try {
                    storageService.delete(orphan);
                    deleted++;
                } catch (Exception e) {
                    log.warn("Failed to delete orphaned file '{}': {}", orphan, e.getMessage());
                }
            }
        }

        log.info("Media sweep: {} disk files, {} referenced keys, {} orphans ({} deleted)",
                diskKeys.size(), referenced.size(), orphans.size(), deleted);
        return new SweepReport(diskKeys.size(), referenced.size(), orphans, deleted);
    }

    private void fillMetadata(MediaFile mediaFile) {
        try {
            Resource resource = storageService.load(mediaFile.getFileKey());
            if (mediaFile.getSizeBytes() == null) {
                mediaFile.setSizeBytes(resource.contentLength());
            }
            if (mediaFile.getMimeType() == null) {
                mediaFile.setMimeType(storageService.getContentType(mediaFile.getFileKey()));
            }
        } catch (Exception e) {
            log.warn("Failed to stat file '{}': {}", mediaFile.getFileKey(), e.getMessage());
        }
    }

    private List<DeadReference> findDeadReferences(Set<String> diskKeys) {
        List<DeadReference> dead = new ArrayList<>();
        for (MediaColumn mediaColumn : MEDIA_COLUMNS) {
            jdbcTemplate.query(
                    "SELECT uuid, " + mediaColumn.column() + " FROM " + mediaColumn.table()
                            + " WHERE " + mediaColumn.column() + " IS NOT NULL",
                    rs -> {
                        String value = rs.getString(2);
                        String key = FileUrlResolver.toKey(value);
                        if (key != null && !diskKeys.contains(key)) {
                            dead.add(new DeadReference(mediaColumn.table(), mediaColumn.column(),
                                    UUID.fromString(rs.getString(1)), value));
                        }
                    });
        }
        return dead;
    }

    /**
     * Removes lost-file entries from assignment_submissions.file_urls arrays,
     * NULLing the column when nothing survives.
     */
    private int pruneDeadSubmissionFileUrls(Set<String> diskKeys) {
        List<Map.Entry<UUID, String[]>> updates = new ArrayList<>();
        jdbcTemplate.query("SELECT uuid, file_urls FROM assignment_submissions WHERE file_urls IS NOT NULL", rs -> {
            String[] urls = (String[]) rs.getArray(2).getArray();
            String[] kept = Arrays.stream(urls)
                    .filter(url -> {
                        String key = FileUrlResolver.toKey(url);
                        return key == null || diskKeys.contains(key);
                    })
                    .toArray(String[]::new);
            if (kept.length != urls.length) {
                updates.add(Map.entry(UUID.fromString(rs.getString(1)), kept));
            }
        });
        for (Map.Entry<UUID, String[]> update : updates) {
            String[] kept = update.getValue();
            jdbcTemplate.update(
                    "UPDATE assignment_submissions SET file_urls = ? WHERE uuid = ?",
                    ps -> {
                        if (kept.length == 0) {
                            ps.setNull(1, java.sql.Types.ARRAY);
                        } else {
                            ps.setArray(1, ps.getConnection().createArrayOf("text", kept));
                        }
                        ps.setObject(2, update.getKey());
                    });
        }
        return updates.size();
    }

    private Set<String> collectReferencedKeys() {
        Set<String> keys = new HashSet<>();
        for (MediaColumn mediaColumn : MEDIA_COLUMNS) {
            jdbcTemplate.query(
                    "SELECT " + mediaColumn.column() + " FROM " + mediaColumn.table()
                            + " WHERE " + mediaColumn.column() + " IS NOT NULL",
                    rs -> {
                        String key = FileUrlResolver.toKey(rs.getString(1));
                        if (key != null) {
                            keys.add(key);
                        }
                    });
        }
        jdbcTemplate.query("SELECT unnest(file_urls) FROM assignment_submissions WHERE file_urls IS NOT NULL", rs -> {
            String key = FileUrlResolver.toKey(rs.getString(1));
            if (key != null) {
                keys.add(key);
            }
        });
        return keys;
    }
}
