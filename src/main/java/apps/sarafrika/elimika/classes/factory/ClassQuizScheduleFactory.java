package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassQuizScheduleDTO;
import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassQuizScheduleFactory {

    public static ClassQuizScheduleDTO toDTO(ClassQuizSchedule entity) {
        if (entity == null) {
            return null;
        }
        return new ClassQuizScheduleDTO(
                entity.getUuid(),
                entity.getClassDefinitionUuid(),
                entity.getLessonUuid(),
                entity.getQuizUuid(),
                entity.getClassLessonPlanUuid(),
                entity.getVisibleAt(),
                entity.getDueAt(),
                entity.getTimezone(),
                entity.getReleaseStrategy(),
                entity.getTimeLimitOverride(),
                entity.getAttemptLimitOverride(),
                entity.getPassingScoreOverride(),
                entity.getInstructorUuid(),
                entity.getNotes(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static ClassQuizSchedule toEntity(ClassQuizScheduleDTO dto) {
        if (dto == null) {
            return null;
        }
        ClassQuizSchedule entity = new ClassQuizSchedule();
        entity.setUuid(dto.uuid());
        entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        entity.setLessonUuid(dto.lessonUuid());
        entity.setQuizUuid(dto.quizUuid());
        entity.setClassLessonPlanUuid(dto.classLessonPlanUuid());
        entity.setVisibleAt(dto.visibleAt());
        entity.setDueAt(dto.dueAt());
        entity.setTimezone(dto.timezone());
        entity.setReleaseStrategy(dto.releaseStrategy() != null ? dto.releaseStrategy() : ClassAssessmentReleaseStrategy.INHERITED);
        entity.setTimeLimitOverride(dto.timeLimitOverride());
        entity.setAttemptLimitOverride(dto.attemptLimitOverride());
        entity.setPassingScoreOverride(dto.passingScoreOverride());
        entity.setInstructorUuid(dto.instructorUuid());
        entity.setNotes(dto.notes());
        return entity;
    }

    public static void updateEntityFromDTO(ClassQuizSchedule entity, ClassQuizScheduleDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (dto.classDefinitionUuid() != null) {
            entity.setClassDefinitionUuid(dto.classDefinitionUuid());
        }
        if (dto.lessonUuid() != null) {
            entity.setLessonUuid(dto.lessonUuid());
        }
        if (dto.quizUuid() != null) {
            entity.setQuizUuid(dto.quizUuid());
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
        if (dto.timezone() != null) {
            entity.setTimezone(dto.timezone());
        }
        if (dto.releaseStrategy() != null) {
            entity.setReleaseStrategy(dto.releaseStrategy());
        }
        if (dto.timeLimitOverride() != null) {
            entity.setTimeLimitOverride(dto.timeLimitOverride());
        }
        if (dto.attemptLimitOverride() != null) {
            entity.setAttemptLimitOverride(dto.attemptLimitOverride());
        }
        if (dto.passingScoreOverride() != null) {
            entity.setPassingScoreOverride(dto.passingScoreOverride());
        }
        if (dto.instructorUuid() != null) {
            entity.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.notes() != null) {
            entity.setNotes(dto.notes());
        }
    }
}
