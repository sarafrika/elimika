package apps.sarafrika.elimika.course.dto.request;

public record CreateLessonRequestDTO(
        String title,
        String description,
        String content,
        int lessonOrder,
        boolean isPublished,
        Long classId
) {
}

