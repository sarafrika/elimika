package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Optional<Withdrawal> findByUuidAndOwnerTypeAndOwnerUuid(UUID uuid, WalletOwnerType ownerType, UUID ownerUuid);

    Optional<Withdrawal> findByOwnerTypeAndOwnerUuidAndIdempotencyKey(WalletOwnerType ownerType, UUID ownerUuid,
                                                                      String idempotencyKey);

    Optional<Withdrawal> findByUuid(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Withdrawal w where w.uuid = :uuid")
    Optional<Withdrawal> lockByUuid(@Param("uuid") UUID uuid);

    @Query("select w from Withdrawal w where w.ownerType = :ownerType and w.ownerUuid = :ownerUuid "
            + "and w.status in :statuses order by w.id desc")
    Page<Withdrawal> findByOwner(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                                 @Param("statuses") Collection<WithdrawalStatus> statuses, Pageable pageable);

    @Query("select w from Withdrawal w where w.status in :statuses order by w.id desc")
    Page<Withdrawal> findByStatusIn(@Param("statuses") Collection<WithdrawalStatus> statuses, Pageable pageable);

    @Query("select coalesce(sum(w.amount), 0) from Withdrawal w where w.ownerType = :ownerType "
            + "and w.ownerUuid = :ownerUuid and w.currencyCode = :currency and w.status in :statuses")
    BigDecimal sumByStatus(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                           @Param("currency") String currency, @Param("statuses") Collection<WithdrawalStatus> statuses);

    @Query("select coalesce(sum(w.amount), 0) from Withdrawal w where w.ownerType = :ownerType "
            + "and w.ownerUuid = :ownerUuid and w.status in :statuses and w.createdDate >= :since")
    BigDecimal sumSince(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                        @Param("statuses") Collection<WithdrawalStatus> statuses,
                        @Param("since") java.time.LocalDateTime since);

    boolean existsByPayoutMethodUuidAndStatus(UUID payoutMethodUuid, WithdrawalStatus status);

    @Query("select w.uuid from Withdrawal w where w.status = :status order by w.id")
    List<UUID> findUuidsByStatus(@Param("status") WithdrawalStatus status);

    @Query("select w.uuid from Withdrawal w where w.status = :status and w.createdDate < :before order by w.id")
    List<UUID> findUuidsByStatusCreatedBefore(@Param("status") WithdrawalStatus status,
                                              @Param("before") java.time.LocalDateTime before);

    long countByStatus(WithdrawalStatus status);

    @Query(value = "select nextval('withdrawal_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();

    @Query("select coalesce(sum(w.netAmount), 0) from Withdrawal w where w.status = :status and w.completedAt >= :since")
    BigDecimal sumNetCompletedSince(@Param("status") WithdrawalStatus status, @Param("since") OffsetDateTime since);
}
