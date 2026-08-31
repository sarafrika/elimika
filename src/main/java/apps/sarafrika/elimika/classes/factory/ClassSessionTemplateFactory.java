package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassSessionTemplateFactory {

    public static ClassSessionTemplateDTO toDTO(ClassSessionTemplate entity) {
        if (entity == null) {
            return null;
        }
        return new ClassSessionTemplateDTO(
                entity.getUuid(),
                entity.getStartTime(),
                entity.getEndTime(),
                toRecurrenceDTO(entity),
                Optional.ofNullable(entity.getConflictResolution()).orElse(ConflictResolutionStrategy.FAIL)
        );
    }

    public static List<ClassSessionTemplateDTO> toDTOList(List<ClassSessionTemplate> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(ClassSessionTemplateFactory::toDTO)
                .toList();
    }

    public static ClassSessionTemplate toEntity(UUID classDefinitionUuid,
                                                ClassSessionTemplateDTO dto,
                                                int templateOrder) {
        if (dto == null) {
            return null;
        }
        ClassSessionTemplate entity = new ClassSessionTemplate();
        entity.setUuid(dto.uuid());
        entity.setClassDefinitionUuid(classDefinitionUuid);
        entity.setTemplateOrder(templateOrder);
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        if (dto.recurrence() != null && dto.recurrence().recurrenceType() != null) {
            entity.setRecurrenceType(ClassRecurrenceType.fromValue(dto.recurrence().recurrenceType().name()));
            entity.setIntervalValue(dto.recurrence().intervalValue());
            entity.setDaysOfWeek(dto.recurrence().daysOfWeek());
            entity.setDayOfMonth(dto.recurrence().dayOfMonth());
            entity.setEndDate(dto.recurrence().endDate());
            entity.setOccurrenceCount(dto.recurrence().occurrenceCount());
        }
        entity.setConflictResolution(Optional.ofNullable(dto.conflictResolution())
                .orElse(ConflictResolutionStrategy.FAIL));
        return entity;
    }

    private static ClassRecurrenceDTO toRecurrenceDTO(ClassSessionTemplate entity) {
        if (entity.getRecurrenceType() == null) {
            return null;
        }
        return new ClassRecurrenceDTO(
                ClassRecurrenceDTO.RecurrenceType.valueOf(entity.getRecurrenceType().name()),
                entity.getIntervalValue(),
                entity.getDaysOfWeek(),
                entity.getDayOfMonth(),
                entity.getEndDate(),
                entity.getOccurrenceCount()
        );
    }
}
