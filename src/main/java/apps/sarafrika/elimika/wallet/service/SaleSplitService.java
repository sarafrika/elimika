package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.dto.SaleSplitDTO;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The breakdown of each captured sale line behind a wallet's SALE and PLATFORM_FEE rows. The split itself is
 * recorded on the purchase line at settlement and read through {@code shared.spi.revenue.SaleSplitQueryService};
 * this names the parties and links the wallet rows.
 */
public interface SaleSplitService {

    /** The owner's paid splits, newest first, with the other party named and the owner's own rows linked. */
    Page<SaleSplitDTO> salesOf(WalletOwner owner, Pageable pageable);

    /**
     * The splits that paid into one purse: a person's COURSE_CREATOR purse is their creator legs, INSTRUCTOR their
     * delivery legs, STUDENT none; your_* then covers that leg only. A null purse is every purse.
     */
    Page<SaleSplitDTO> salesOf(WalletOwner owner, WalletPurse purse, Pageable pageable);

    /** One of the owner's splits; not found when the owner was not paid on it. */
    SaleSplitDTO saleOf(WalletOwner owner, UUID splitUuid);

    /** One of the owner's splits as seen from one purse; not found when that purse was not paid on it. */
    SaleSplitDTO saleOf(WalletOwner owner, WalletPurse purse, UUID splitUuid);

    /** Every split of an order, for admins. */
    List<SaleSplitDTO> splitsOfOrder(String orderId);

    /** One split, for admins. */
    SaleSplitDTO split(UUID splitUuid);

    /** The split each SALE or PLATFORM_FEE row belongs to, by transaction uuid; other rows are left out. */
    Map<UUID, UUID> splitUuidsOf(Collection<UserWalletTransaction> transactions);
}
