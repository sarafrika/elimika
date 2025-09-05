package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.model.RecurrencePattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurrencePatternFactory {

    public static RecurrencePatternDTO toDTO(RecurrencePattern entity) {
        if (entity == null) {
            return null;
        }
        return new RecurrencePatternDTO(
                entity.getUuid(),
                entity.getRecurrenceType(),
                entity.getIntervalValue(),
                entity.getDaysOfWeek(),
                entity.getDayOfMonth(),
                entity.getEndDate(),
                entity.getOccurrenceCount()
        );
    }

    public static RecurrencePattern toEntity(RecurrencePatternDTO dto) {
        if (dto == null) {
            return null;
        }
        RecurrencePattern entity = new RecurrencePattern();
        entity.setUuid(dto.uuid());
        entity.setRecurrenceType(dto.recurrenceType());
        entity.setIntervalValue(dto.intervalValue());
        entity.setDaysOfWeek(dto.daysOfWeek());
        entity.setDayOfMonth(dto.dayOfMonth());
        entity.setEndDate(dto.endDate());
        entity.setOccurrenceCount(dto.occurrenceCount());
        return entity;
    }

    public static void updateEntityFromDTO(RecurrencePattern entity, RecurrencePatternDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.recurrenceType() != null) {
            entity.setRecurrenceType(dto.recurrenceType());
        }
        if (dto.intervalValue() != null) {
            entity.setIntervalValue(dto.intervalValue());
        }
        if (dto.daysOfWeek() != null) {
            entity.setDaysOfWeek(dto.daysOfWeek());
        }
        if (dto.dayOfMonth() != null) {
            entity.setDayOfMonth(dto.dayOfMonth());
        }
        if (dto.endDate() != null) {
            entity.setEndDate(dto.endDate());
        }
        if (dto.occurrenceCount() != null) {
            entity.setOccurrenceCount(dto.occurrenceCount());
        }
    }
}