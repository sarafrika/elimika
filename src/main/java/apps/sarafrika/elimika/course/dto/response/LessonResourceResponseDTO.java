package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.LessonResource;

public record LessonResourceResponseDTO(
        Long id,

        String title,

        String resourceUrl,

        int displayOrder
) {

    public static LessonResourceResponseDTO from(LessonResource lessonResource) {

        return new LessonResourceResponseDTO(
                lessonResource.getId(),
                lessonResource.getTitle(),
                lessonResource.getResourceUrl(),
                lessonResource.getDisplayOrder());
    }
}
