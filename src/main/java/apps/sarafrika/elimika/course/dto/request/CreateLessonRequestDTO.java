package apps.sarafrika.elimika.course.dto.request;

import java.util.List;

public record CreateLessonRequestDTO(
        String title,

        String description,

        int lessonOrder,

        boolean isPublished,

        List<CreateLessonContentDTO> content,

        List<CreateLessonResourceRequestDTO> resources
) {
}

