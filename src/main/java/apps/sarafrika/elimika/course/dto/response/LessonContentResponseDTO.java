package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.LessonContent;

public record LessonContentResponseDTO(
        Long id,

        String title,

        int displayOrder,

        int duration,

        String contentType
) {

    public static LessonContentResponseDTO from(LessonContent lessonContent, String contentType) {

        return new LessonContentResponseDTO(
                lessonContent.getId(),
                lessonContent.getTitle(),
                lessonContent.getDisplayOrder(),
                lessonContent.getDuration(),
                contentType
        );
    }
}
