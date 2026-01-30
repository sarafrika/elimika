package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorEducation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorEducationFactory {

    public static CourseCreatorEducationDTO toDTO(CourseCreatorEducation education) {
        if (education == null) {
            return null;
        }
        return new CourseCreatorEducationDTO(
                education.getUuid(),
                education.getCourseCreatorUuid(),
                education.getQualification(),
                education.getFieldOfStudy(),
                education.getSchoolName(),
                education.getYearCompleted(),
                education.getCertificateNumber(),
                education.getCreatedDate(),
                education.getCreatedBy(),
                education.getLastModifiedDate(),
                education.getLastModifiedBy()
        );
    }

    public static CourseCreatorEducation toEntity(CourseCreatorEducationDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorEducation education = new CourseCreatorEducation();
        education.setUuid(dto.uuid());
        education.setCourseCreatorUuid(dto.courseCreatorUuid());
        education.setQualification(dto.qualification());
        education.setFieldOfStudy(dto.fieldOfStudy());
        education.setSchoolName(dto.schoolName());
        education.setYearCompleted(dto.yearCompleted());
        education.setCertificateNumber(dto.certificateNumber());
        education.setCreatedDate(dto.createdDate());
        education.setCreatedBy(dto.createdBy());
        education.setLastModifiedDate(dto.updatedDate());
        education.setLastModifiedBy(dto.updatedBy());
        return education;
    }
}
