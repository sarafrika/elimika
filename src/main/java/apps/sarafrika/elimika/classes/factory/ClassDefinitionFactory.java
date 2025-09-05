package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassDefinitionFactory {

    public static ClassDefinitionDTO toDTO(ClassDefinition entity) {
        if (entity == null) {
            return null;
        }
        return new ClassDefinitionDTO(
                entity.getUuid(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDefaultInstructorUuid(),
                entity.getOrganisationUuid(),
                entity.getCourseUuid(),
                entity.getDurationMinutes(),
                entity.getLocationType(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist(),
                entity.getRecurrencePatternUuid(),
                entity.getIsActive(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy()
        );
    }

    public static ClassDefinition toEntity(ClassDefinitionDTO dto) {
        if (dto == null) {
            return null;
        }
        ClassDefinition entity = new ClassDefinition();
        entity.setUuid(dto.uuid());
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setDefaultInstructorUuid(dto.defaultInstructorUuid());
        entity.setOrganisationUuid(dto.organisationUuid());
        entity.setCourseUuid(dto.courseUuid());
        entity.setDurationMinutes(dto.durationMinutes());
        entity.setLocationType(dto.locationType());
        entity.setMaxParticipants(dto.maxParticipants());
        entity.setAllowWaitlist(dto.allowWaitlist());
        entity.setRecurrencePatternUuid(dto.recurrencePatternUuid());
        entity.setIsActive(dto.isActive());
        return entity;
    }

    public static void updateEntityFromDTO(ClassDefinition entity, ClassDefinitionDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.title() != null) {
            entity.setTitle(dto.title());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
        if (dto.defaultInstructorUuid() != null) {
            entity.setDefaultInstructorUuid(dto.defaultInstructorUuid());
        }
        if (dto.organisationUuid() != null) {
            entity.setOrganisationUuid(dto.organisationUuid());
        }
        if (dto.courseUuid() != null) {
            entity.setCourseUuid(dto.courseUuid());
        }
        if (dto.durationMinutes() != null) {
            entity.setDurationMinutes(dto.durationMinutes());
        }
        if (dto.locationType() != null) {
            entity.setLocationType(dto.locationType());
        }
        if (dto.maxParticipants() != null) {
            entity.setMaxParticipants(dto.maxParticipants());
        }
        if (dto.allowWaitlist() != null) {
            entity.setAllowWaitlist(dto.allowWaitlist());
        }
        if (dto.recurrencePatternUuid() != null) {
            entity.setRecurrencePatternUuid(dto.recurrencePatternUuid());
        }
        if (dto.isActive() != null) {
            entity.setIsActive(dto.isActive());
        }
    }
}