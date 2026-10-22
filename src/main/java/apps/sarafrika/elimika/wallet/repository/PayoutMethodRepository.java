package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutMethodRepository extends JpaRepository<PayoutMethod, Long> {

    @Query("select m from PayoutMethod m where m.ownerType = :ownerType and m.ownerUuid = :ownerUuid "
            + "and m.status <> :removed order by m.defaultMethod desc, m.id asc")
    List<PayoutMethod> findLive(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                                @Param("removed") PayoutMethodStatus removed);

    Optional<PayoutMethod> findByUuidAndOwnerTypeAndOwnerUuid(UUID uuid, WalletOwnerType ownerType, UUID ownerUuid);
}
