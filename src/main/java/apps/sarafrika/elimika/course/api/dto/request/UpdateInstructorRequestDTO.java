package apps.sarafrika.elimika.course.api.dto.request;

public record UpdateInstructorRequestDTO(
        String firstName,
        String otherNames,
        String lastName,
        String email,
        String bio
) {
}
