package apps.sarafrika.elimika.timetabling.factory;

import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StudentScheduleFactory {

    public static StudentScheduleDTO toDTO(ScheduledInstance scheduledInstance, Enrollment enrollment) {
        if (scheduledInstance == null || enrollment == null) {
            return null;
        }
        return new StudentScheduleDTO(
                enrollment.getUuid(),
                scheduledInstance.getUuid(),
                scheduledInstance.getClassDefinitionUuid(),
                scheduledInstance.getInstructorUuid(),
                scheduledInstance.getTitle(),
                scheduledInstance.getStartTime(),
                scheduledInstance.getEndTime(),
                scheduledInstance.getTimezone(),
                scheduledInstance.getLocationType(),
                scheduledInstance.getLocationName(),
                scheduledInstance.getLocationLatitude(),
                scheduledInstance.getLocationLongitude(),
                scheduledInstance.getStatus(),
                enrollment.getStatus(),
                enrollment.getAttendanceMarkedAt()
        );
    }

    public static List<StudentScheduleDTO> toDTOList(List<ScheduledInstance> scheduledInstances, List<Enrollment> enrollments) {
        if (scheduledInstances == null || enrollments == null) {
            return null;
        }
        
        return scheduledInstances.stream()
                .flatMap(instance -> 
                    enrollments.stream()
                            .filter(enrollment -> enrollment.getScheduledInstanceUuid().equals(instance.getUuid()))
                            .map(enrollment -> toDTO(instance, enrollment))
                )
                .collect(Collectors.toList());
    }
}
