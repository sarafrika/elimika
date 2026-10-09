package apps.sarafrika.elimika.shared.storage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StoragePathUtils {

    /** Derived resized image variants; generated on demand, never registry-tracked. */
    public static final String VARIANTS_FOLDER = "variants";
    public static final Set<Integer> VARIANT_WIDTHS = Set.of(320, 640, 1280);

    /** Base key of an original's resized variant; the stored file may carry a .jpg/.png suffix. */
    public static String variantBase(String key, int width) {
        return VARIANTS_FOLDER + "/w" + width + "/" + key;
    }

    /** Every key a variant of {@code key} at {@code width} may be stored under. */
    public static List<String> variantKeys(String key, int width) {
        String base = variantBase(key, width);
        return List.of(base + ".jpg", base + ".png", base);
    }

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
