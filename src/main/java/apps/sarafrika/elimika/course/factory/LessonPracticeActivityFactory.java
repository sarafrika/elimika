package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.LessonPracticeActivityDTO;
import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonPracticeActivityFactory {

    public static LessonPracticeActivityDTO toDTO(LessonPracticeActivity activity) {
        if (activity == null) {
            return null;
        }
        return new LessonPracticeActivityDTO(
                activity.getUuid(),
                activity.getLessonUuid(),
                activity.getTitle(),
                activity.getInstructions(),
                activity.getActivityType(),
                activity.getGrouping(),
                activity.getEstimatedMinutes(),
                activity.getMaterials(),
                activity.getExpectedOutput(),
                activity.getDisplayOrder(),
                activity.getStatus(),
                activity.getActive(),
                activity.getCreatedDate(),
                activity.getCreatedBy(),
                activity.getLastModifiedDate(),
                activity.getLastModifiedBy()
        );
    }

    public static LessonPracticeActivity toEntity(LessonPracticeActivityDTO dto) {
        if (dto == null) {
            return null;
        }
        LessonPracticeActivity activity = new LessonPracticeActivity();
        activity.setUuid(dto.uuid());
        activity.setLessonUuid(dto.lessonUuid());
        activity.setTitle(dto.title());
        activity.setInstructions(dto.instructions());
        activity.setActivityType(dto.activityType());
        activity.setGrouping(dto.grouping());
        activity.setEstimatedMinutes(dto.estimatedMinutes());
        activity.setMaterials(dto.materials());
        activity.setExpectedOutput(dto.expectedOutput());
        activity.setDisplayOrder(dto.displayOrder());
        activity.setStatus(dto.status());
        activity.setActive(dto.active());
        activity.setCreatedDate(dto.createdDate());
        activity.setCreatedBy(dto.createdBy());
        activity.setLastModifiedDate(dto.updatedDate());
        activity.setLastModifiedBy(dto.updatedBy());
        return activity;
    }
}
