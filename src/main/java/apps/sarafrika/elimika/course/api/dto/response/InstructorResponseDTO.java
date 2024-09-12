package apps.sarafrika.elimika.course.api.dto.response;

import apps.sarafrika.elimika.course.domain.Instructor;

public record InstructorResponseDTO(
        Long id,
        String name,
        String email,
        String bio
) {
    public static InstructorResponseDTO from(Instructor instructor) {

        return new InstructorResponseDTO(
                instructor.getId(),
                instructor.getName(),
                instructor.getEmail(),
                instructor.getBio());
    }
}
