package apps.sarafrika.elimika.shared.storage.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Central registry of every file persisted through the storage layer.
 * <p>
 * The {@code fileKey} is the canonical bare storage key relative to the storage root
 * (e.g. {@code course_thumbnails/9f1b101a.jpg}). Domain tables keep their own reference
 * columns holding the same key; this registry adds file metadata, ownership and
 * existence tracking so orphaned or lost files can be detected and reconciled.
 */
@Entity
@Table(name = "media_files")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class MediaFile extends BaseEntity {

    @Column(name = "file_key", nullable = false, unique = true)
    private String fileKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    /**
     * False when the registry knows about the key but the file is absent from disk
     * (detected during reconciliation). Serving such a key would 404.
     */
    @Builder.Default
    @Column(name = "file_exists", nullable = false)
    private boolean fileExists = true;
}
