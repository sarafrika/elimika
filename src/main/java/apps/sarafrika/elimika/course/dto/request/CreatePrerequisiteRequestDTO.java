package apps.sarafrika.elimika.course.dto.request;

public record CreatePrerequisiteRequestDTO(
        Long prerequisiteTypeId,

        Long courseId,

        Long requiredForCourseId,

        double minimumScore
) {
}
