package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/** The four columns a schedule summary reads from a scheduled instance, without the entity. */
public record ScheduleSlice(UUID classDefinitionUuid, SchedulingStatus status, LocalDateTime startTime,
                            LocalDateTime endTime) {
}
