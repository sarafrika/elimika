package apps.sarafrika.elimika.course.dto.request;

public record CreateLessonContentDTO(
        String title,

        int displayOrder,

        int duration,

        String contentType,

        String contentText
) {
}
