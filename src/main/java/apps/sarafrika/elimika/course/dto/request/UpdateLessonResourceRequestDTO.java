package apps.sarafrika.elimika.course.dto.request;

public record UpdateLessonResourceRequestDTO(
        Long id,

        String title,

        String resourceUrl,

        int displayOrder
) {
}
