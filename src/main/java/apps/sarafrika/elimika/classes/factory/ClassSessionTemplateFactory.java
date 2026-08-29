package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassRecurrenceDTO;
import apps.sarafrika.elimika.classes.dto.ClassSessionTemplateDTO;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
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
                durationMinutes(entity.getStartTime(), entity.getEndTime()),
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
        ClassSessionTemplateDTO effectiveDto = dto.withDurationApplied(null);
        ClassSessionTemplate entity = new ClassSessionTemplate();
        entity.setUuid(effectiveDto.uuid());
        entity.setClassDefinitionUuid(classDefinitionUuid);
        entity.setTemplateOrder(templateOrder);
        entity.setStartTime(effectiveDto.startTime());
        entity.setEndTime(effectiveDto.endTime());
        if (effectiveDto.recurrence() != null && effectiveDto.recurrence().recurrenceType() != null) {
            entity.setRecurrenceType(ClassRecurrenceType.fromValue(effectiveDto.recurrence().recurrenceType().name()));
            entity.setIntervalValue(effectiveDto.recurrence().intervalValue());
            entity.setDaysOfWeek(effectiveDto.recurrence().daysOfWeek());
            entity.setDayOfMonth(effectiveDto.recurrence().dayOfMonth());
            entity.setEndDate(effectiveDto.recurrence().endDate());
            entity.setOccurrenceCount(effectiveDto.recurrence().occurrenceCount());
        }
        entity.setConflictResolution(Optional.ofNullable(effectiveDto.conflictResolution())
                .orElse(ConflictResolutionStrategy.FAIL));
        return entity;
    }

    private static Integer durationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return null;
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return minutes > Integer.MAX_VALUE ? null : Math.toIntExact(minutes);
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
