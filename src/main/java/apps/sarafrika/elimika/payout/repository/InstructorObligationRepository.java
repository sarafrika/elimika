package apps.sarafrika.elimika.payout.repository;

import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.model.InstructorObligation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for instructor obligations.
 * <p>
 * The two aggregate queries are the replacement for the old in-memory
 * {@code training_fee x completed_sessions} accumulator. They sum rows that were written at the rate
 * that stood on the day, so the answer is stable no matter what the rate card says now.
 * <p>
 * Both are handed the statuses to consider rather than naming them inline, because the attribute is
 * persisted through an {@code AttributeConverter} and a bound parameter is converted for certain,
 * where an inline HQL enum literal depends on Hibernate choosing to.
 */
public interface InstructorObligationRepository extends JpaRepository<InstructorObligation, Long> {

    Optional<InstructorObligation> findByUuid(UUID uuid);

    /**
     * The idempotency pre-check for accrual: one obligation per session, per instructor. The unique
     * constraint {@code uq_instructor_obligations_session} is what actually enforces this; this
     * lookup only spares the common case a failed insert.
     */
    /**
     * Any obligation already accrued for this instructor, on this class, for that calendar date.
     * A per-day class is sold as one day regardless of how many sessions fall in it, so a second
     * session on the same date must not earn a second day's pay.
     */
    Optional<InstructorObligation> findFirstByClassDefinitionUuidAndInstructorUuidAndSessionDate(
            UUID classDefinitionUuid, UUID instructorUuid, java.time.LocalDate sessionDate);

    Optional<InstructorObligation> findByClassDefinitionUuidAndSessionUuidAndInstructorUuid(
            UUID classDefinitionUuid, UUID sessionUuid, UUID instructorUuid);

    Page<InstructorObligation> findByOrganisationUuid(UUID organisationUuid, Pageable pageable);

    Page<InstructorObligation> findByOrganisationUuidAndStatus(
            UUID organisationUuid, InstructorObligationStatus status, Pageable pageable);

    Page<InstructorObligation> findByOrganisationUuidAndInstructorUuid(
            UUID organisationUuid, UUID instructorUuid, Pageable pageable);

    Page<InstructorObligation> findByOrganisationUuidAndInstructorUuidAndStatus(
            UUID organisationUuid, UUID instructorUuid, InstructorObligationStatus status, Pageable pageable);

    Page<InstructorObligation> findByInstructorUserUuid(UUID instructorUserUuid, Pageable pageable);

    /**
     * What an organisation owes, grouped by instructor and currency.
     *
     * @param countedStatuses the statuses that count as delivered work; cancelled and disputed rows
     *                        are left out by the caller, so neither the money nor the session shows up
     * @param outstandingStatus the status that means "still unpaid"
     */
    @Query("""
            select new apps.sarafrika.elimika.payout.repository.InstructorPayableAggregate(
                o.instructorUuid,
                o.currencyCode,
                coalesce(sum(case when o.status = :outstandingStatus then o.rateAmount else 0 end), 0),
                coalesce(sum(case when o.status = :settledStatus then o.rateAmount else 0 end), 0),
                count(distinct o.classDefinitionUuid),
                count(o.id),
                count(case when o.status = :outstandingStatus then o.id end)
            )
            from InstructorObligation o
            where o.organisationUuid = :organisationUuid
              and o.status in :countedStatuses
            group by o.instructorUuid, o.currencyCode
            order by o.instructorUuid, o.currencyCode
            """)
    List<InstructorPayableAggregate> aggregateByOrganisation(
            @Param("organisationUuid") UUID organisationUuid,
            @Param("countedStatuses") Collection<InstructorObligationStatus> countedStatuses,
            @Param("outstandingStatus") InstructorObligationStatus outstandingStatus,
            @Param("settledStatus") InstructorObligationStatus settledStatus);

    /**
     * Money actually paid out to instructors, month by month. Sums {@code rate_amount} for
     * obligations the organisation has settled, from a cut-off onward, grouped by the month
     * they were settled in and by currency. {@code year()}/{@code month()} are used in place of
     * {@code to_char} so the grouping stays portable; the caller formats the label. The status is
     * a bound parameter for the same {@code AttributeConverter} reason as the aggregate above.
     *
     * @param settledStatus the status that means the money has left the organisation
     * @param since         the earliest settlement instant to include
     */
    @Query("""
            select new apps.sarafrika.elimika.payout.repository.MonthlyPayoutAggregate(
                year(o.settledAt),
                month(o.settledAt),
                o.currencyCode,
                coalesce(sum(o.rateAmount), 0)
            )
            from InstructorObligation o
            where o.organisationUuid = :organisationUuid
              and o.status = :settledStatus
              and o.settledAt >= :since
            group by year(o.settledAt), month(o.settledAt), o.currencyCode
            order by year(o.settledAt), month(o.settledAt)
            """)
    List<MonthlyPayoutAggregate> aggregateMonthlySettlementsByOrganisation(
            @Param("organisationUuid") UUID organisationUuid,
            @Param("settledStatus") InstructorObligationStatus settledStatus,
            @Param("since") LocalDateTime since);

    /**
     * What an instructor is owed, grouped by the organisation that owes it and by currency.
     */
    @Query("""
            select new apps.sarafrika.elimika.payout.repository.InstructorStatementAggregate(
                o.organisationUuid,
                o.currencyCode,
                coalesce(sum(case when o.status = :outstandingStatus then o.rateAmount else 0 end), 0),
                coalesce(sum(case when o.status = :settledStatus then o.rateAmount else 0 end), 0),
                count(o.id),
                count(case when o.status = :outstandingStatus then o.id end)
            )
            from InstructorObligation o
            where o.instructorUserUuid = :instructorUserUuid
              and o.status in :countedStatuses
            group by o.organisationUuid, o.currencyCode
            order by o.organisationUuid, o.currencyCode
            """)
    List<InstructorStatementAggregate> aggregateByInstructorUser(
            @Param("instructorUserUuid") UUID instructorUserUuid,
            @Param("countedStatuses") Collection<InstructorObligationStatus> countedStatuses,
            @Param("outstandingStatus") InstructorObligationStatus outstandingStatus,
            @Param("settledStatus") InstructorObligationStatus settledStatus);
}
