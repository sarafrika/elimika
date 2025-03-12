package apps.sarafrika.elimika.course.dto.request;

public record UpdatePrerequisiteRequestDTO(
        Long prerequisiteTypeId,

        Long courseId,

        Long requiredForCourseId,

        double minimumScore
) {
}
