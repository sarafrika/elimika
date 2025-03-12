package apps.sarafrika.elimika.course.dto.response;

public record PrerequisiteResponseDTO(
        Long id,

        PrerequisiteTypeResponseDTO prerequisiteType,

        CourseResponseDTO course,

        CourseResponseDTO requiredForCourse,

        double minimumScore
) {
}
