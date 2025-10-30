package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassAssignmentScheduleDTO;
import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassAssignmentScheduleFactory {

    public static ClassAssignmentScheduleDTO toDTO(ClassAssignmentSchedule entity) {
        if (entity == null) {
            return null;
        }
        return new ClassAssignmentScheduleDTO(
                entity.getUuid(),
                entity.getClassDefinitionUuid(),
                entity.getLessonUuid(),
                entity.getAssignmentUuid(),
                entity.getClassLessonPlanUuid(),
                entity.getVisibleAt(),
                entity.getDueAt(),
                entity.getGradingDueAt(),
                entity.getTimezone(),
                entity.getReleaseStrategy(),
                entity.getMaxAttempts(),
                entity.getInstructorUuid(),
                entity.getNotes(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static ClassAssignmentSchedule toEntity(ClassAssignmentScheduleDTO dto) {
        if (dto == null) {
            return null;
        }
        ClassAssignmentSchedule entity = new ClassAssignmentSchedule();
        entity.setUuid(dto.uuid());
        entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        entity.setLessonUuid(dto.lessonUuid());
        entity.setAssignmentUuid(dto.assignmentUuid());
        entity.setClassLessonPlanUuid(dto.classLessonPlanUuid());
        entity.setVisibleAt(dto.visibleAt());
        entity.setDueAt(dto.dueAt());
        entity.setGradingDueAt(dto.gradingDueAt());
        entity.setTimezone(dto.timezone());
        entity.setReleaseStrategy(dto.releaseStrategy() != null ? dto.releaseStrategy() : ClassAssessmentReleaseStrategy.INHERITED);
        entity.setMaxAttempts(dto.maxAttempts());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setNotes(dto.notes());
        return entity;
    }

    public static void updateEntityFromDTO(ClassAssignmentSchedule entity, ClassAssignmentScheduleDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (dto.classDefinitionUuid() != null) {
            entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        }
        if (dto.lessonUuid() != null) {
            entity.setLessonUuid(dto.lessonUuid());
        }
        if (dto.assignmentUuid() != null) {
            entity.setAssignmentUuid(dto.assignmentUuid());
        }
        if (dto.classLessonPlanUuid() != null) {
            entity.setClassLessonPlanUuid(dto.classLessonPlanUuid());
        }
        if (dto.visibleAt() != null) {
            entity.setVisibleAt(dto.visibleAt());
        }
        if (dto.dueAt() != null) {
            entity.setDueAt(dto.dueAt());
        }
        if (dto.gradingDueAt() != null) {
            entity.setGradingDueAt(dto.gradingDueAt());
        }
        if (dto.timezone() != null) {
            entity.setTimezone(dto.timezone());
        }
        if (dto.releaseStrategy() != null) {
            entity.setReleaseStrategy(dto.releaseStrategy());
        }
        if (dto.maxAttempts() != null) {
            entity.setMaxAttempts(dto.maxAttempts());
        }
        if (dto.instructorUuid() != null) {
            entity.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.notes() != null) {
            entity.setNotes(dto.notes());
        }
    }
}
