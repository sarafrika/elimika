package apps.sarafrika.elimika.shared.spi.revenue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Reads the split recorded on each settled purchase line, for wallet screens that explain a sale. */
public interface SaleSplitQueryService {

    /** Lines that paid this party on either leg, newest settlement first; the pageable's sort is ignored. */
    Page<SaleSplitView> findPaidTo(SalePartyType ownerType, UUID ownerUuid, Pageable pageable);

    /** A settled line with a recorded split, by the line's uuid. */
    Optional<SaleSplitView> findByUuid(UUID splitUuid);

    /** Every settled line of an order that has a recorded split, in line order. */
    List<SaleSplitView> findByOrderId(String orderId);

    /** The split uuid per {@code orderId:lineItemId} reference; references with no recorded split are left out. */
    Map<String, UUID> findSplitUuidsByLineReference(Collection<String> lineReferences);
}
