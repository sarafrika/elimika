package apps.sarafrika.elimika.course.dto.request;

public record UpdateLessonRequestDTO(
        String title,
        String description,
        String content,
        int lessonOrder,
        boolean isPublished
) {
}

