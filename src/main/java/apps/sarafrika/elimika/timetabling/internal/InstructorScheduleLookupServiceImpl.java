package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleEntry;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleLookupService;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorScheduleStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InstructorScheduleLookupServiceImpl implements InstructorScheduleLookupService {

    private final TimetableService timetableService;

    @Override
    public List<InstructorScheduleEntry> getScheduleForInstructor(UUID instructorUuid, LocalDate startDate, LocalDate endDate) {
        List<ScheduledInstanceDTO> scheduledInstances = timetableService.getScheduleForInstructor(instructorUuid, startDate, endDate);
        return scheduledInstances.stream()
                .map(this::toScheduleEntry)
                .toList();
    }

    private InstructorScheduleEntry toScheduleEntry(ScheduledInstanceDTO instance) {
        return new InstructorScheduleEntry(
                instance.uuid(),
                instance.startTime(),
                instance.endTime(),
                instance.status() != null ? InstructorScheduleStatus.fromValue(instance.status().name()) : null,
                instance.title(),
                instance.classDefinitionUuid(),
                instance.locationType(),
                instance.cancellationReason()
        );
    }
}
