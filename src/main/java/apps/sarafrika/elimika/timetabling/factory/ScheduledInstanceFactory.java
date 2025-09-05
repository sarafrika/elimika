package apps.sarafrika.elimika.timetabling.factory;

import apps.sarafrika.elimika.timetabling.dto.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.dto.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.util.enums.SchedulingStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduledInstanceFactory {

    public static ScheduledInstanceDTO toDTO(ScheduledInstance entity) {
        if (entity == null) {
            return null;
        }
        return new ScheduledInstanceDTO(
                entity.getUuid(),
                entity.getClassDefinitionUuid(),
                entity.getInstructorUuid(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getTimezone(),
                entity.getTitle(),
                entity.getLocationType(),
                entity.getMaxParticipants(),
                entity.getStatus(),
                entity.getCancellationReason(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy()
        );
    }

    public static ScheduledInstance toEntity(ScheduleRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        ScheduledInstance entity = new ScheduledInstance();
        entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setTimezone(dto.timezone() != null ? dto.timezone() : "UTC");
        entity.setStatus(SchedulingStatus.SCHEDULED);
        return entity;
    }

    public static ScheduledInstance toEntity(ScheduledInstanceDTO dto) {
        if (dto == null) {
            return null;
        }
        ScheduledInstance entity = new ScheduledInstance();
        entity.setUuid(dto.uuid());
        entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setTimezone(dto.timezone());
        entity.setTitle(dto.title());
        entity.setLocationType(dto.locationType());
        entity.setMaxParticipants(dto.maxParticipants());
        entity.setStatus(dto.status());
        entity.setCancellationReason(dto.cancellationReason());
        return entity;
    }

    public static void updateEntityFromDTO(ScheduledInstance entity, ScheduledInstanceDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.classDefinitionUuid() != null) {
            entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        }
        if (dto.instructorUuid() != null) {
            entity.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.startTime() != null) {
            entity.setStartTime(dto.startTime());
        }
        if (dto.endTime() != null) {
            entity.setEndTime(dto.endTime());
        }
        if (dto.timezone() != null) {
            entity.setTimezone(dto.timezone());
        }
        if (dto.title() != null) {
            entity.setTitle(dto.title());
        }
        if (dto.locationType() != null) {
            entity.setLocationType(dto.locationType());
        }
        if (dto.maxParticipants() != null) {
            entity.setMaxParticipants(dto.maxParticipants());
        }
        if (dto.status() != null) {
            entity.setStatus(dto.status());
        }
        if (dto.cancellationReason() != null) {
            entity.setCancellationReason(dto.cancellationReason());
        }
    }

    public static List<ScheduledInstanceDTO> toDTOList(List<ScheduledInstance> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ScheduledInstanceFactory::toDTO)
                .collect(Collectors.toList());
    }
}