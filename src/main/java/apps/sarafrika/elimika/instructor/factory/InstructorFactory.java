package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import apps.sarafrika.elimika.instructor.model.Instructor;

public class InstructorFactory {
    private InstructorFactory() {
    }

    // Convert Instructor entity to InstructorDTO
    public static InstructorDTO toDTO(Instructor instructor) {
        if (instructor == null) {
            return null;
        }
        return new InstructorDTO(
                instructor.getUuid(),
                instructor.getUserUuid(),
                instructor.getFullName(),
                instructor.getLatitude(),
                instructor.getLongitude(),
                instructor.getWebsite(),
                instructor.getBio(),
                instructor.getProfessionalHeadline(),
                null,
                null,
                null,
                instructor.getCreatedDate(),
                instructor.getCreatedBy(),
                instructor.getLastModifiedDate(),
                instructor.getLastModifiedBy()
        );
    }

    // Convert InstructorDTO to Instructor entity
    public static Instructor toEntity(InstructorDTO dto) {
        if (dto == null) {
            return null;
        }
        Instructor instructor = new Instructor();
        instructor.setUuid(dto.uuid());
        instructor.setUserUuid(dto.userUuid());
        return instructor;
    }
}
