package apps.sarafrika.elimika.availability.factory;

import apps.sarafrika.elimika.availability.dto.*;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AvailabilityFactory {

    public static AvailabilitySlotDTO toDTO(InstructorAvailability entity) {
        if (entity == null) {
            return null;
        }
        return new AvailabilitySlotDTO(
                entity.getUuid(),
                entity.getInstructorUuid(),
                entity.getAvailabilityType(),
                entity.getDayOfWeek(),
                entity.getDayOfMonth(),
                entity.getSpecificDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCustomPattern(),
                entity.getIsAvailable(),
                entity.getRecurrenceInterval(),
                entity.getEffectiveStartDate(),
                entity.getEffectiveEndDate(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getColorCode()
        );
    }

    public static InstructorAvailability toEntity(AvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorAvailability entity = new InstructorAvailability();
        entity.setUuid(dto.uuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setAvailabilityType(dto.availabilityType());
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setDayOfMonth(dto.dayOfMonth());
        entity.setSpecificDate(dto.specificDate());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setCustomPattern(dto.customPattern());
        entity.setIsAvailable(dto.isAvailable());
        entity.setRecurrenceInterval(dto.recurrenceInterval());
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        return entity;
    }

    public static InstructorAvailability toEntity(WeeklyAvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorAvailability entity = new InstructorAvailability();
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setAvailabilityType(AvailabilityType.WEEKLY);
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setIsAvailable(dto.isAvailable() != null ? dto.isAvailable() : true);
        entity.setRecurrenceInterval(dto.recurrenceInterval() != null ? dto.recurrenceInterval() : 1);
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        return entity;
    }

    public static InstructorAvailability toEntity(DailyAvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorAvailability entity = new InstructorAvailability();
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setAvailabilityType(AvailabilityType.DAILY);
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setIsAvailable(dto.isAvailable() != null ? dto.isAvailable() : true);
        entity.setRecurrenceInterval(dto.recurrenceInterval() != null ? dto.recurrenceInterval() : 1);
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        return entity;
    }

    public static InstructorAvailability toEntity(MonthlyAvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorAvailability entity = new InstructorAvailability();
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setAvailabilityType(AvailabilityType.MONTHLY);
        entity.setDayOfMonth(dto.dayOfMonth());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setIsAvailable(dto.isAvailable() != null ? dto.isAvailable() : true);
        entity.setRecurrenceInterval(dto.recurrenceInterval() != null ? dto.recurrenceInterval() : 1);
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        return entity;
    }

    public static InstructorAvailability toEntity(CustomAvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorAvailability entity = new InstructorAvailability();
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setAvailabilityType(AvailabilityType.CUSTOM);
        entity.setCustomPattern(dto.customPattern());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setIsAvailable(dto.isAvailable() != null ? dto.isAvailable() : true);
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        return entity;
    }

    public static void updateEntityFromDTO(InstructorAvailability entity, AvailabilitySlotDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.instructorUuid() != null) {
            entity.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.availabilityType() != null) {
            entity.setAvailabilityType(dto.availabilityType());
        }
        if (dto.dayOfWeek() != null) {
            entity.setDayOfWeek(dto.dayOfWeek());
        }
        if (dto.dayOfMonth() != null) {
            entity.setDayOfMonth(dto.dayOfMonth());
        }
        if (dto.specificDate() != null) {
            entity.setSpecificDate(dto.specificDate());
        }
        if (dto.startTime() != null) {
            entity.setStartTime(dto.startTime());
        }
        if (dto.endTime() != null) {
            entity.setEndTime(dto.endTime());
        }
        if (dto.customPattern() != null) {
            entity.setCustomPattern(dto.customPattern());
        }
        if (dto.isAvailable() != null) {
            entity.setIsAvailable(dto.isAvailable());
        }
        if (dto.recurrenceInterval() != null) {
            entity.setRecurrenceInterval(dto.recurrenceInterval());
        }
        if (dto.effectiveStartDate() != null) {
            entity.setEffectiveStartDate(dto.effectiveStartDate());
        }
        if (dto.effectiveEndDate() != null) {
            entity.setEffectiveEndDate(dto.effectiveEndDate());
        }
        if (dto.colorCode() != null) {
            entity.setColorCode(dto.colorCode());
        }
    }

    public static List<AvailabilitySlotDTO> toDTOList(List<InstructorAvailability> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(AvailabilityFactory::toDTO)
                .collect(Collectors.toList());
    }
}