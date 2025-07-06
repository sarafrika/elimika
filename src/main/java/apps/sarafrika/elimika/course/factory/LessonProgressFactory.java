package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.LessonProgressDTO;
import apps.sarafrika.elimika.course.dto.ContentProgressDTO;
import apps.sarafrika.elimika.course.model.LessonProgress;
import apps.sarafrika.elimika.course.model.ContentProgress;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonProgressFactory {

    // Convert LessonProgress entity to LessonProgressDTO
    public static LessonProgressDTO toDTO(LessonProgress lessonProgress) {
        if (lessonProgress == null) {
            return null;
        }
        return new LessonProgressDTO(
                lessonProgress.getUuid(),
                lessonProgress.getEnrollmentUuid(),
                lessonProgress.getLessonUuid(),
                lessonProgress.getStatus(),
                lessonProgress.getStartedAt(),
                lessonProgress.getCompletedAt(),
                lessonProgress.getTimeSpentMinutes(),
                lessonProgress.getCreatedDate(),
                lessonProgress.getCreatedBy(),
                lessonProgress.getLastModifiedDate(),
                lessonProgress.getLastModifiedBy()
        );
    }

    // Convert LessonProgressDTO to LessonProgress entity
    public static LessonProgress toEntity(LessonProgressDTO dto) {
        if (dto == null) {
            return null;
        }
        LessonProgress lessonProgress = new LessonProgress();
        lessonProgress.setUuid(dto.uuid());
        lessonProgress.setEnrollmentUuid(dto.enrollmentUuid());
        lessonProgress.setLessonUuid(dto.lessonUuid());
        lessonProgress.setStatus(dto.status());
        lessonProgress.setStartedAt(dto.startedAt());
        lessonProgress.setCompletedAt(dto.completedAt());
        lessonProgress.setTimeSpentMinutes(dto.timeSpentMinutes());
        lessonProgress.setCreatedDate(dto.createdDate());
        lessonProgress.setCreatedBy(dto.createdBy());
        lessonProgress.setLastModifiedDate(dto.updatedDate());
        lessonProgress.setLastModifiedBy(dto.updatedBy());
        return lessonProgress;
    }
}

