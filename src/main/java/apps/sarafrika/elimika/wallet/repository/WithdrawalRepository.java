package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
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

    @Query("select w from Withdrawal w where w.status in :statuses and w.payoutBatchUuid is null order by w.id desc")
    Page<Withdrawal> findUnbatchedByStatusIn(@Param("statuses") Collection<WithdrawalStatus> statuses,
                                             Pageable pageable);

    @Query("select w from Withdrawal w where w.payoutBatchUuid = :batchUuid order by w.id")
    List<Withdrawal> findByPayoutBatchUuid(@Param("batchUuid") UUID batchUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Withdrawal w where w.payoutBatchUuid = :batchUuid order by w.id")
    List<Withdrawal> lockByPayoutBatchUuid(@Param("batchUuid") UUID batchUuid);

    /** Row count and net total of a batch, as {count, sum}. */
    @Query("select count(w), coalesce(sum(w.netAmount), 0) from Withdrawal w where w.payoutBatchUuid = :batchUuid")
    List<Object[]> batchTotals(@Param("batchUuid") UUID batchUuid);

    /** Rows per batch and status, as {batch uuid, status, count}. */
    @Query("select w.payoutBatchUuid, w.status, count(w) from Withdrawal w where w.payoutBatchUuid in :batchUuids "
            + "group by w.payoutBatchUuid, w.status")
    List<Object[]> countByBatchAndStatus(@Param("batchUuids") Collection<UUID> batchUuids);

    /** Withdrawals of released batches waiting to be handed to M-Pesa, oldest batch and row first. */
    @Query("select w.uuid from Withdrawal w, PayoutBatch b where w.payoutBatchUuid = b.uuid and b.status = :batchStatus "
            + "and w.status = :status order by b.releasedAt, b.id, w.id")
    List<UUID> findUuidsInBatchesByStatus(@Param("batchStatus") PayoutBatchStatus batchStatus,
                                          @Param("status") WithdrawalStatus status, Pageable pageable);

    long countByStatusAndPayoutBatchUuidIsNotNull(WithdrawalStatus status);

    @Query("select w.uuid from Withdrawal w where w.status = :status and w.payoutBatchUuid is null order by w.id")
    List<UUID> findUnbatchedUuidsByStatus(@Param("status") WithdrawalStatus status);

    @Query("select coalesce(sum(w.amount), 0) from Withdrawal w where w.ownerType = :ownerType "
            + "and w.ownerUuid = :ownerUuid and w.currencyCode = :currency and w.status in :statuses")
    BigDecimal sumByStatus(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                           @Param("currency") String currency, @Param("statuses") Collection<WithdrawalStatus> statuses);

    /** Gross amount per purse, as {purse, sum}, of an owner's withdrawals in the given statuses. */
    @Query("select w.purse, coalesce(sum(w.amount), 0) from Withdrawal w where w.ownerType = :ownerType "
            + "and w.ownerUuid = :ownerUuid and w.currencyCode = :currency and w.status in :statuses group by w.purse")
    List<Object[]> sumByPurseAndStatus(@Param("ownerType") WalletOwnerType ownerType, @Param("ownerUuid") UUID ownerUuid,
                                       @Param("currency") String currency,
                                       @Param("statuses") Collection<WithdrawalStatus> statuses);

    /** Across every purse of the owner: the daily limit is per person, so moving money between purses cannot lift it. */
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

    /**
     * Per owner, currency and purse: the held withdrawals' count and gross amount, and the wallet debits
     * recorded for them (withdrawal and fee rows by reference in any of the owner's purses, less reversals), in
     * one statement.
     */
    @Query(value = """
            select w.owner_type    as "ownerType",
                   w.owner_uuid    as "ownerUuid",
                   w.currency_code as "currencyCode",
                   w.purse         as "purse",
                   count(*)        as "heldCount",
                   coalesce(sum(w.amount), 0) as "heldAmount",
                   coalesce(sum(d.debited), 0) as "heldDebited"
            from withdrawals w
                     left join lateral (
                select coalesce(sum(case when t.transaction_type = 'WITHDRAWAL_REVERSAL' then -t.amount
                                         else t.amount end), 0) as debited
                from user_wallet_transactions t
                         join user_wallets uw on uw.id = t.wallet_id
                where uw.owner_type = w.owner_type
                  and uw.user_uuid = w.owner_uuid
                  and uw.currency_code = w.currency_code
                  and ((t.transaction_type = 'WITHDRAWAL' and t.reference = w.reference)
                    or (t.transaction_type = 'PAYOUT_FEE' and t.reference = w.reference || ':fee')
                    or (t.transaction_type = 'WITHDRAWAL_REVERSAL' and t.reference = w.reference || ':reversal'))
                ) d on true
            where w.owner_uuid in (:ownerUuids)
              and w.status in (:statuses)
            group by w.owner_type, w.owner_uuid, w.currency_code, w.purse
            """, nativeQuery = true)
    List<HeldWithdrawalTotals> heldTotalsByOwner(@Param("ownerUuids") Collection<UUID> ownerUuids,
                                                 @Param("statuses") Collection<String> statuses);

    @Query("select w from Withdrawal w where w.payoutBatchUuid in :batchUuids order by w.id")
    List<Withdrawal> findByPayoutBatchUuidIn(@Param("batchUuids") Collection<UUID> batchUuids);
}
