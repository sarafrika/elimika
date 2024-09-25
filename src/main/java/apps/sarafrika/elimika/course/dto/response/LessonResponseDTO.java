package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Lesson;

public record LessonResponseDTO(
        Long id,
        String title,
        String description,
        String content,
        int lessonOrder
) {
    public static LessonResponseDTO from(Lesson lesson) {

        return new LessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getContent(),
                lesson.getLessonOrder()
        );
    }
}

