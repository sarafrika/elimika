package apps.sarafrika.elimika.timetabling.factory;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnrollmentFactory {

    public static EnrollmentDTO toDTO(Enrollment entity) {
        if (entity == null) {
            return null;
        }
        return new EnrollmentDTO(
                entity.getUuid(),
                entity.getScheduledInstanceUuid(),
                entity.getStudentUuid(),
                entity.getStatus(),
                entity.getAttendanceMarkedAt(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy()
        );
    }

    public static Enrollment toEntity(UUID scheduledInstanceUuid, UUID studentUuid) {
        if (scheduledInstanceUuid == null || studentUuid == null) {
            return null;
        }
        Enrollment entity = new Enrollment();
        entity.setScheduledInstanceUuid(scheduledInstanceUuid);
        entity.setStudentUuid(studentUuid);
        entity.setStatus(EnrollmentStatus.ENROLLED);
        return entity;
    }

    public static Enrollment toEntity(EnrollmentDTO dto) {
        if (dto == null) {
            return null;
        }
        Enrollment entity = new Enrollment();
        entity.setUuid(dto.uuid());
        entity.setScheduledInstanceUuid(dto.scheduledInstanceUuid());
        entity.setStudentUuid(dto.studentUuid());
        entity.setStatus(dto.status());
        entity.setAttendanceMarkedAt(dto.attendanceMarkedAt());
        return entity;
    }

    public static void updateEntityFromDTO(Enrollment entity, EnrollmentDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.scheduledInstanceUuid() != null) {
            entity.setScheduledInstanceUuid(dto.scheduledInstanceUuid());
        }
        if (dto.studentUuid() != null) {
            entity.setStudentUuid(dto.studentUuid());
        }
        if (dto.status() != null) {
            entity.setStatus(dto.status());
        }
        if (dto.attendanceMarkedAt() != null) {
            entity.setAttendanceMarkedAt(dto.attendanceMarkedAt());
        }
    }

    public static List<EnrollmentDTO> toDTOList(List<Enrollment> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(EnrollmentFactory::toDTO)
                .collect(Collectors.toList());
    }
}
