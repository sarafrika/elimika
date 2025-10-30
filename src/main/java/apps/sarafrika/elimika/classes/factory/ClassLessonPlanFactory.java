package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassLessonPlanDTO;
import apps.sarafrika.elimika.classes.model.ClassLessonPlan;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassLessonPlanFactory {

    public static ClassLessonPlanDTO toDTO(ClassLessonPlan entity) {
        if (entity == null) {
            return null;
        }
        return new ClassLessonPlanDTO(
                entity.getUuid(),
                entity.getClassDefinitionUuid(),
                entity.getLessonUuid(),
                entity.getScheduledStart(),
                entity.getScheduledEnd(),
                entity.getScheduledInstanceUuid(),
                entity.getInstructorUuid(),
                entity.getNotes(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static ClassLessonPlan toEntity(ClassLessonPlanDTO dto) {
        if (dto == null) {
            return null;
        }
        ClassLessonPlan entity = new ClassLessonPlan();
        entity.setUuid(dto.uuid());
        entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        entity.setLessonUuid(dto.lessonUuid());
        entity.setScheduledStart(dto.scheduledStart());
        entity.setScheduledEnd(dto.scheduledEnd());
        entity.setScheduledInstanceUuid(dto.scheduledInstanceUuid());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setNotes(dto.notes());
        return entity;
    }

    public static void updateEntityFromDTO(ClassLessonPlan entity, ClassLessonPlanDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (dto.classDefinitionUuid() != null) {
            entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        }
        if (dto.lessonUuid() != null) {
            entity.setLessonUuid(dto.lessonUuid());
        }
        if (dto.scheduledStart() != null) {
            entity.setScheduledStart(dto.scheduledStart());
        }
        if (dto.scheduledEnd() != null) {
            entity.setScheduledEnd(dto.scheduledEnd());
        }
        if (dto.scheduledInstanceUuid() != null) {
            entity.setScheduledInstanceUuid(dto.scheduledInstanceUuid());
        }
        if (dto.instructorUuid() != null) {
            entity.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.notes() != null) {
            entity.setNotes(dto.notes());
        }
    }
}
