package apps.sarafrika.elimika.shared.tracking.discovery;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One item a recommender showed, in the order it was shown.
 *
 * @param itemType    the kind of item, lowercase with underscores (e.g. {@code course}, {@code marketplace_job})
 * @param itemUuid    the item's UUID
 * @param position    0-based rank in the response
 * @param reasonCodes machine-readable reasons the item was chosen (e.g. {@code SKILL_GAP}); never free text
 */
public record DiscoveryImpression(String itemType, UUID itemUuid, int position, List<String> reasonCodes) {

    public DiscoveryImpression {
        Objects.requireNonNull(itemType, "itemType");
        Objects.requireNonNull(itemUuid, "itemUuid");
        if (position < 0) {
            throw new IllegalArgumentException("position must be 0 or greater");
        }
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
