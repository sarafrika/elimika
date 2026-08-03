package apps.sarafrika.elimika.payout.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregated obligations for one instructor in one currency, as computed by the database.
 * <p>
 * Boxed types throughout: JPQL constructor expressions bind {@code count()} as {@code Long} and a
 * conditional {@code sum()} as {@code BigDecimal}, and an unboxed parameter list would simply fail to
 * match at startup.
 */
public record InstructorPayableAggregate(
        UUID instructorUuid,
        String currencyCode,
        BigDecimal amountOutstanding,
        BigDecimal amountSettled,
        Long classCount,
        Long sessionCount,
        Long outstandingSessionCount
) {
}
