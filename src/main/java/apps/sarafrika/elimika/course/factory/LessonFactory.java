package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.model.Lesson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Lesson Factory
 * <p>
 * Factory class responsible for converting between Lesson entities and LessonDTO objects.
 * Provides centralized conversion logic to ensure consistency across the application.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonFactory {

    public static LessonDTO toDTO(Lesson lesson) {
        if (lesson == null) {
            return null;
        }

        return new LessonDTO(
                lesson.getUuid(),
                lesson.getLessonNo(),
                lesson.getCourseUuid(),
                lesson.getLessonName(),
                lesson.getLessonDescription(),
                lesson.getLessonType(),
                lesson.getEstimatedDurationMinutes(),
                lesson.getCreatedDate(),
                lesson.getLastModifiedDate(),
                lesson.getCreatedBy(),
                lesson.getLastModifiedBy()
        );
    }

    public static Lesson toEntity(LessonDTO lessonDTO) {
        if (lessonDTO == null) {
            return null;
        }

        Lesson lesson = new Lesson();
        lesson.setLessonNo(lessonDTO.lessonNo());
        lesson.setCourseUuid(lessonDTO.courseUuid());
        lesson.setLessonName(lessonDTO.lessonName());
        lesson.setLessonDescription(lessonDTO.lessonDescription());
        lesson.setLessonType(lessonDTO.lessonType());
        lesson.setEstimatedDurationMinutes(lessonDTO.estimatedDurationMinutes());

        return lesson;
    }
}