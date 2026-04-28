package apps.sarafrika.elimika.shared.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cross-module contract for resolving scheduled class duration summaries.
 */
public interface ClassScheduleService {

    /**
     * Returns the total scheduled minutes and instance count for a class definition.
     *
     * @param classDefinitionUuid class definition identifier
     * @return summary of scheduled minutes and instances
     */
    ClassScheduleSummary getScheduleSummary(UUID classDefinitionUuid);

    record ClassScheduleSummary(
            long scheduledMinutes,
            long scheduledInstances,
            long completedSessions,
            BigDecimal classProgressPercentage
    ) {
        public ClassScheduleSummary(long scheduledMinutes, long scheduledInstances) {
            this(scheduledMinutes, scheduledInstances, 0, BigDecimal.ZERO);
        }
    }
}
