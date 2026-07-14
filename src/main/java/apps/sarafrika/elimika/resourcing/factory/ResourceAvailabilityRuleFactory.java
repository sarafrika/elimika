package apps.sarafrika.elimika.resourcing.factory;

import apps.sarafrika.elimika.resourcing.dto.ResourceAvailabilityRuleDTO;
import apps.sarafrika.elimika.resourcing.model.ResourceAvailabilityRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceAvailabilityRuleFactory {

    public static ResourceAvailabilityRuleDTO toDTO(ResourceAvailabilityRule entity) {
        if (entity == null) {
            return null;
        }
        return new ResourceAvailabilityRuleDTO(
                entity.getUuid(),
                entity.getResourceUuid(),
                entity.getRuleType(),
                entity.getDaysOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getSpecificStart(),
                entity.getSpecificEnd(),
                entity.getEffectiveStartDate(),
                entity.getEffectiveEndDate(),
                entity.getNotes()
        );
    }

    public static List<ResourceAvailabilityRuleDTO> toDTOList(List<ResourceAvailabilityRule> entities) {
        return entities == null ? List.of() : entities.stream().map(ResourceAvailabilityRuleFactory::toDTO).toList();
    }

    public static ResourceAvailabilityRule toEntity(java.util.UUID resourceUuid, ResourceAvailabilityRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        ResourceAvailabilityRule entity = new ResourceAvailabilityRule();
        entity.setResourceUuid(resourceUuid);
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    public static void updateEntityFromDTO(ResourceAvailabilityRule entity, ResourceAvailabilityRuleDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setRuleType(dto.ruleType());
        entity.setDaysOfWeek(dto.daysOfWeek());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setSpecificStart(dto.specificStart());
        entity.setSpecificEnd(dto.specificEnd());
        entity.setEffectiveStartDate(dto.effectiveStartDate());
        entity.setEffectiveEndDate(dto.effectiveEndDate());
        entity.setNotes(dto.notes());
    }
}
