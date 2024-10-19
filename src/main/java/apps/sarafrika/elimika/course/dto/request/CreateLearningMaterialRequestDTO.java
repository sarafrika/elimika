package apps.sarafrika.elimika.course.dto.request;

public record CreateLearningMaterialRequestDTO(
        String title,

        String type,

        String url,

        Long courseId,

        Long lessonId
) {
}
