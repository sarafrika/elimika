package apps.sarafrika.elimika.instructor.persistence;

import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorRequestDTO;

public class InstructorFactory {

    public static Instructor create(CreateInstructorRequestDTO createInstructorRequestDTO) {
        String name = createFullName(
                createInstructorRequestDTO.firstName(),
                createInstructorRequestDTO.otherNames(),
                createInstructorRequestDTO.lastName()
        );

        return Instructor.builder()
                .name(name)
                .email(createInstructorRequestDTO.email())
                .bio(createInstructorRequestDTO.bio())
                .build();
    }

    public static void update(Instructor instructor, UpdateInstructorRequestDTO updateInstructorRequestDTO) {
        String name = createFullName(
                updateInstructorRequestDTO.firstName(),
                updateInstructorRequestDTO.otherNames(),
                updateInstructorRequestDTO.lastName()
        );

        instructor.setName(name);
        instructor.setEmail(updateInstructorRequestDTO.email());
        instructor.setBio(updateInstructorRequestDTO.bio());
    }

    private static String createFullName(String firstName, String otherNames, String lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(firstName);

        if (otherNames != null && !otherNames.isEmpty()) {
            nameBuilder.append(" ").append(otherNames);
        }

        nameBuilder.append(" ").append(lastName);

        return nameBuilder.toString();
    }
}
