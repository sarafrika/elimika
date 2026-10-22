package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletAuditEventRepository extends JpaRepository<WalletAuditEvent, Long> {

    @Query("select event from WalletAuditEvent event where event.ownerType = :ownerType "
            + "and event.ownerUuid = :ownerUuid order by event.id desc")
    Page<WalletAuditEvent> findByOwner(@Param("ownerType") WalletOwnerType ownerType,
                                       @Param("ownerUuid") UUID ownerUuid,
                                       Pageable pageable);
}
