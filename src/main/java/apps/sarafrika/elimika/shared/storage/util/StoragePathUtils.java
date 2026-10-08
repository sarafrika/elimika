package apps.sarafrika.elimika.shared.storage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StoragePathUtils {

    /** Derived resized image variants; generated on demand, never registry-tracked. */
    public static final String VARIANTS_FOLDER = "variants";

    public static String normalizeRelativePath(String filePath) {
        if (filePath == null) {
            return null;
        }

        String normalizedPath = filePath.trim().replace('\\', '/');

        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }
}
