package apps.sarafrika.elimika.instructor.dto.request;

public record UpdateInstructorRequestDTO(
        String firstName,
        String otherNames,
        String lastName,
        String email,
        String bio
) {
}
