package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.spi.InstructorDTO;
import apps.sarafrika.elimika.instructor.model.Instructor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorFactory {

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
                instructor.getAdminVerified(),
                instructor.getWebsite(),
                instructor.getBio(),
                instructor.getProfessionalHeadline(),
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
        instructor.setFullName(dto.fullName());
        instructor.setLatitude(dto.latitude());
        instructor.setLongitude(dto.longitude());
        instructor.setAdminVerified(dto.verified());
        instructor.setWebsite(dto.website());
        instructor.setBio(dto.bio());
        instructor.setProfessionalHeadline(dto.professionalHeadline());
        instructor.setCreatedDate(dto.createdDate());
        instructor.setCreatedBy(dto.createdBy());
        instructor.setLastModifiedDate(dto.updatedDate());
        instructor.setLastModifiedBy(dto.updatedBy());
        return instructor;
    }
}