package apps.sarafrika.elimika.course.dto.request;

public record UpdateLessonContentDTO(
        String title,

        int displayOrder,

        int duration,

        String contentType
) {
}
