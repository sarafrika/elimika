package apps.sarafrika.elimika.instructor.dto.response;


import apps.sarafrika.elimika.instructor.persistence.Instructor;

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
