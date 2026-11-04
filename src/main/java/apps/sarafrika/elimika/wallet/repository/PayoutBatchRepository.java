package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.PayoutBatch;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {

    Optional<PayoutBatch> findByUuid(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from PayoutBatch b where b.uuid = :uuid")
    Optional<PayoutBatch> lockByUuid(@Param("uuid") UUID uuid);

    @Query("select b from PayoutBatch b where b.status in :statuses order by b.id desc")
    Page<PayoutBatch> findByStatusIn(@Param("statuses") Collection<PayoutBatchStatus> statuses, Pageable pageable);

    @Query("select b.uuid from PayoutBatch b where b.status = :status order by b.releasedAt, b.id")
    List<UUID> findUuidsByStatus(@Param("status") PayoutBatchStatus status);
}
