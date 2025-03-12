package apps.sarafrika.elimika.course.dto.request;

public record CreateLessonResourceRequestDTO(
        String title,

        String resourceUrl,

        int displayOrder
) {
}
