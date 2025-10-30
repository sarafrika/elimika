package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.model.Lesson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonFactory {

    // Convert Lesson entity to LessonDTO
    public static LessonDTO toDTO(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        return new LessonDTO(
                lesson.getUuid(),
                lesson.getCourseUuid(),
                lesson.getLessonNumber(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getLearningObjectives(),
                lesson.getStatus(),
                lesson.getActive(),
                lesson.getCreatedDate(),
                lesson.getCreatedBy(),
                lesson.getLastModifiedDate(),
                lesson.getLastModifiedBy()
        );
    }

    // Convert LessonDTO to Lesson entity
    public static Lesson toEntity(LessonDTO dto) {
        if (dto == null) {
            return null;
        }
        Lesson lesson = new Lesson();
        lesson.setUuid(dto.uuid());
        lesson.setCourseUuid(dto.courseUuid());
        lesson.setLessonNumber(dto.lessonNumber());
        lesson.setTitle(dto.title());
        lesson.setDescription(dto.description());
        lesson.setLearningObjectives(dto.learningObjectives());
        lesson.setStatus(dto.status());
        lesson.setActive(dto.active());
        lesson.setCreatedDate(dto.createdDate());
        lesson.setCreatedBy(dto.createdBy());
        lesson.setLastModifiedDate(dto.updatedDate());
        lesson.setLastModifiedBy(dto.updatedBy());
        return lesson;
    }
}
