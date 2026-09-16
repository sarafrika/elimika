package apps.sarafrika.elimika.tenancy.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only view of a training branch's identity and location pin for other modules.
 */
public record BranchLocation(
        UUID uuid,
        UUID organisationUuid,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active
) {

    public boolean hasPin() {
        return latitude != null && longitude != null;
    }
}
