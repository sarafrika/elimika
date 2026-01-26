package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Timetabling-backed implementation of {@link ClassScheduleService}.
 */
@Service
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ScheduledInstanceRepository scheduledInstanceRepository;

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleSummary getScheduleSummary(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return new ClassScheduleSummary(0, 0);
        }

        List<ScheduledInstance> instances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        if (instances.isEmpty()) {
            return new ClassScheduleSummary(0, 0);
        }

        long totalMinutes = 0;
        long instanceCount = 0;
        for (ScheduledInstance instance : instances) {
            if (!isCountableStatus(instance)) {
                continue;
            }
            if (instance.getStartTime() == null || instance.getEndTime() == null) {
                continue;
            }
            if (!instance.getEndTime().isAfter(instance.getStartTime())) {
                continue;
            }

            long minutes = Duration.between(instance.getStartTime(), instance.getEndTime()).toMinutes();
            if (minutes <= 0) {
                continue;
            }
            totalMinutes += minutes;
            instanceCount++;
        }

        return new ClassScheduleSummary(totalMinutes, instanceCount);
    }

    private boolean isCountableStatus(ScheduledInstance instance) {
        if (instance == null || instance.getStatus() == null) {
            return false;
        }
        SchedulingStatus status = instance.getStatus();
        return SchedulingStatus.SCHEDULED.equals(status)
                || SchedulingStatus.ONGOING.equals(status)
                || SchedulingStatus.COMPLETED.equals(status);
    }
}
