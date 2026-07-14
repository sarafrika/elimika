package apps.sarafrika.elimika.resourcing.spi;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A scheduled instance's concrete time window, used to link recruitment holds
 * to the scheduled instances created at instructor assignment.
 */
public record InstanceWindow(UUID scheduledInstanceUuid,
                             LocalDateTime startTime,
                             LocalDateTime endTime) {
}
