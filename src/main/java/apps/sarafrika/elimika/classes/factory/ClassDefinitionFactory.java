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
                entity.getTrainingFee(),
                entity.getClassVisibility(),
                entity.getSessionFormat(),
                entity.getDefaultStartTime(),
                entity.getDefaultEndTime(),
                entity.getLocationType(),
                entity.getLocationName(),
                entity.getLocationLatitude(),
                entity.getLocationLongitude(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist(),
                entity.getRecurrencePatternUuid(),
                entity.getIsActive(),
                java.util.List.of(),
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
        entity.setTrainingFee(dto.trainingFee());
        entity.setClassVisibility(dto.classVisibility());
        entity.setSessionFormat(dto.sessionFormat());
        entity.setDefaultStartTime(dto.defaultStartTime());
        entity.setDefaultEndTime(dto.defaultEndTime());
        entity.setLocationType(dto.locationType());
        entity.setLocationName(dto.locationName());
        entity.setLocationLatitude(dto.locationLatitude());
        entity.setLocationLongitude(dto.locationLongitude());
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
        if (dto.trainingFee() != null) {
            entity.setTrainingFee(dto.trainingFee());
        }
        if (dto.classVisibility() != null) {
            entity.setClassVisibility(dto.classVisibility());
        }
        if (dto.sessionFormat() != null) {
            entity.setSessionFormat(dto.sessionFormat());
        }
        if (dto.defaultStartTime() != null) {
            entity.setDefaultStartTime(dto.defaultStartTime());
        }
        if (dto.defaultEndTime() != null) {
            entity.setDefaultEndTime(dto.defaultEndTime());
        }
        if (dto.locationType() != null) {
            entity.setLocationType(dto.locationType());
        }
        if (dto.locationName() != null) {
            entity.setLocationName(dto.locationName());
        }
        if (dto.locationLatitude() != null) {
            entity.setLocationLatitude(dto.locationLatitude());
        }
        if (dto.locationLongitude() != null) {
            entity.setLocationLongitude(dto.locationLongitude());
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
