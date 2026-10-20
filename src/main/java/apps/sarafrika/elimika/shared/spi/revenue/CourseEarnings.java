package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;
import java.util.UUID;

/** What a course's creator has received from its class sales, net of platform fees. */
public record CourseEarnings(UUID courseUuid, long salesCount, BigDecimal earnedAmount) {
}
