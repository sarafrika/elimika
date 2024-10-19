package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;

public class LessonFactory {

    public static Lesson create(final CreateLessonRequestDTO createLessonRequestDTO) {

        return Lesson.builder()
                .title(createLessonRequestDTO.title())
                .description(createLessonRequestDTO.description())
                .content(createLessonRequestDTO.content())
                .lessonOrder(createLessonRequestDTO.lessonOrder())
                .isPublished(createLessonRequestDTO.isPublished())
                .build();
    }

    public static void update(final Lesson lesson, final UpdateLessonRequestDTO updateLessonRequestDTO) {

        lesson.setTitle(updateLessonRequestDTO.title());
        lesson.setDescription(updateLessonRequestDTO.description());
        lesson.setContent(updateLessonRequestDTO.content());
        lesson.setLessonOrder(updateLessonRequestDTO.lessonOrder());
        lesson.setPublished(updateLessonRequestDTO.isPublished());
    }
}
