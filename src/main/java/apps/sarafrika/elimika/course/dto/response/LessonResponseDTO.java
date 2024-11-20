package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Lesson;

import java.util.List;

public record LessonResponseDTO(
        Long id,

        String title,

        String description,

        int lessonOrder,

        boolean isPublished,

        List<LessonContentResponseDTO> content,

        List<LessonResourceResponseDTO> resources
) {
    public static LessonResponseDTO from(Lesson lesson, List<LessonContentResponseDTO> content, List<LessonResourceResponseDTO> resources) {

        return new LessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getLessonOrder(),
                lesson.isPublished(),
                content,
                resources
        );
    }
}

