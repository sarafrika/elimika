package apps.sarafrika.elimika.shared.spi.payout;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read-only view of what an organisation owes its instructors, aggregated from the persisted
 * obligation ledger owned by the payout module.
 * <p>
 * The interface lives in {@code shared} for the same reason
 * {@link apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService} does: the caller is the
 * {@code classes} module, which serves the organisation payables endpoint, and {@code classes} must
 * not take a dependency on {@code payout}. Obligations flow classes &rarr; payout; a compile-time
 * edge in the other direction would invert that.
 */
public interface InstructorPayableLookupService {

    /**
     * What this organisation currently owes, one entry per instructor and currency.
     * <p>
     * Aggregated from obligation rows, never recomputed from a rate card — an entry exists only
     * because sessions were delivered and recorded at the rate that stood on the day.
     *
     * @param organisationUuid the organisation whose payables are being read
     * @return payables per instructor, empty when nothing has accrued
     */
    List<OrganisationInstructorPayable> findPayablesForOrganisation(UUID organisationUuid);

    /**
     * @param instructorUuid           instructor profile owed
     * @param currencyCode             currency the obligations were accrued in
     * @param amountOutstanding        still owed: accrued rows that are neither settled nor cancelled
     * @param amountSettled            recorded as paid off-platform
     * @param amountAccrued            lifetime total, outstanding plus settled
     * @param classCount               distinct classes that generated the obligations
     * @param sessionCount             sessions that generated them
     * @param outstandingSessionCount  of those, the ones still unpaid
     */
    record OrganisationInstructorPayable(
            UUID instructorUuid,
            String currencyCode,
            BigDecimal amountOutstanding,
            BigDecimal amountSettled,
            BigDecimal amountAccrued,
            long classCount,
            long sessionCount,
            long outstandingSessionCount
    ) {
    }
}
