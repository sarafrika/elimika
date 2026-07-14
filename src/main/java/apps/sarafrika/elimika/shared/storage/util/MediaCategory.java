package apps.sarafrika.elimika.shared.storage.util;

/**
 * Upload categories with their validation rules. Replaces the per-module
 * media validation services that each re-implemented the same checks.
 */
public enum MediaCategory {
    PROFILE_IMAGE("Profile image", 5, Kind.IMAGE),
    THUMBNAIL("Thumbnail", 5, Kind.IMAGE),
    BANNER("Banner", 10, Kind.IMAGE),
    VIDEO("Video", 100, Kind.VIDEO),
    DOCUMENT("Document", 50, Kind.DOCUMENT),
    PDF_DOCUMENT("Document", 50, Kind.PDF),
    /**
     * Safety-net category for flows whose domain service already applied stricter,
     * context-specific validation (e.g. assignment submission types). Size cap matches
     * the global multipart limit.
     */
    ANY("File", 500, Kind.DOCUMENT);

    public enum Kind { IMAGE, VIDEO, DOCUMENT, PDF }

    private final String label;
    private final int maxSizeMb;
    private final Kind kind;

    MediaCategory(String label, int maxSizeMb, Kind kind) {
        this.label = label;
        this.maxSizeMb = maxSizeMb;
        this.kind = kind;
    }

    public String label() {
        return label;
    }

    public long maxSizeBytes() {
        return maxSizeMb * 1024L * 1024L;
    }

    public int maxSizeMb() {
        return maxSizeMb;
    }

    public Kind kind() {
        return kind;
    }
}
