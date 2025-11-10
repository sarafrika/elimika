package apps.sarafrika.elimika.student.factory;

import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.model.Student;

public abstract class StudentFactory {
    // Convert Student entity to StudentDTO
    public static StudentDTO toDTO(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentDTO(
                student.getUuid(),
                student.getUserUuid(),
                student.getDemographicTag(),
                student.getFirstGuardianName(),
                student.getFirstGuardianMobile(),
                student.getSecondGuardianName(),
                student.getSecondGuardianMobile(),
                student.getCreatedDate(),
                student.getCreatedBy(),
                student.getLastModifiedDate(),
                student.getLastModifiedBy()
        );
    }

    // Convert StudentDTO to Student entity
    public static Student toEntity(StudentDTO dto) {
        if (dto == null) {
            return null;
        }
        Student student = new Student();
        student.setUuid(dto.uuid());
        student.setUserUuid(dto.userUuid());
        student.setDemographicTag(dto.demographicTag());
        student.setFirstGuardianName(dto.firstGuardianName());
        student.setFirstGuardianMobile(dto.firstGuardianMobile());
        student.setSecondGuardianName(dto.secondGuardianName());
        student.setSecondGuardianMobile(dto.secondGuardianMobile());
        return student;
    }
}
