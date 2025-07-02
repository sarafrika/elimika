package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorEducationDTO;
import apps.sarafrika.elimika.instructor.model.InstructorEducation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorEducationFactory {

    // Convert InstructorEducation entity to InstructorEducationDTO
    public static InstructorEducationDTO toDTO(InstructorEducation education) {
        if (education == null) {
            return null;
        }
        return new InstructorEducationDTO(
                education.getUuid(),
                education.getInstructorUuid(),
                education.getQualification(),
                education.getSchoolName(),
                education.getYearCompleted(),
                education.getCertificateNumber(),
                education.getCreatedDate(),
                education.getCreatedBy(),
                education.getLastModifiedDate(),
                education.getLastModifiedBy()
        );
    }

    // Convert InstructorEducationDTO to InstructorEducation entity
    public static InstructorEducation toEntity(InstructorEducationDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorEducation education = new InstructorEducation();
        education.setUuid(dto.uuid());
        education.setInstructorUuid(dto.instructorUuid());
        education.setQualification(dto.qualification());
        education.setSchoolName(dto.schoolName());
        education.setYearCompleted(dto.yearCompleted());
        education.setCertificateNumber(dto.certificateNumber());
        return education;
    }
}