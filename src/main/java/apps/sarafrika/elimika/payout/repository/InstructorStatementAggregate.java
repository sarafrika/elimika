package apps.sarafrika.elimika.payout.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregated obligations owed to one instructor by one organisation in one currency.
 * <p>
 * The mirror image of {@link InstructorPayableAggregate}: the organisation view groups by
 * instructor, the instructor view groups by organisation.
 */
public record InstructorStatementAggregate(
        UUID organisationUuid,
        String currencyCode,
        BigDecimal amountOutstanding,
        BigDecimal amountSettled,
        Long sessionCount,
        Long outstandingSessionCount
) {
}
