package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseEnrollmentFactory {

    // Convert CourseEnrollment entity to CourseEnrollmentDTO
    public static CourseEnrollmentDTO toDTO(CourseEnrollment courseEnrollment) {
        if (courseEnrollment == null) {
            return null;
        }
        return new CourseEnrollmentDTO(
                courseEnrollment.getUuid(),
                courseEnrollment.getStudentUuid(),
                courseEnrollment.getCourseUuid(),
                courseEnrollment.getEnrollmentDate(),
                courseEnrollment.getCompletionDate(),
                courseEnrollment.getStatus(),
                courseEnrollment.getProgressPercentage(),
                courseEnrollment.getFinalGrade(),
                courseEnrollment.getCreatedDate(),
                courseEnrollment.getCreatedBy(),
                courseEnrollment.getLastModifiedDate(),
                courseEnrollment.getLastModifiedBy()
        );
    }

    // Convert CourseEnrollmentDTO to CourseEnrollment entity
    public static CourseEnrollment toEntity(CourseEnrollmentDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseEnrollment courseEnrollment = new CourseEnrollment();
        courseEnrollment.setUuid(dto.uuid());
        courseEnrollment.setStudentUuid(dto.studentUuid());
        courseEnrollment.setCourseUuid(dto.courseUuid());
        courseEnrollment.setEnrollmentDate(dto.enrollmentDate());
        courseEnrollment.setCompletionDate(dto.completionDate());
        courseEnrollment.setStatus(dto.status());
        courseEnrollment.setProgressPercentage(dto.progressPercentage());
        courseEnrollment.setFinalGrade(dto.finalGrade());
        courseEnrollment.setCreatedDate(dto.createdDate());
        courseEnrollment.setCreatedBy(dto.createdBy());
        courseEnrollment.setLastModifiedDate(dto.updatedDate());
        courseEnrollment.setLastModifiedBy(dto.updatedBy());
        return courseEnrollment;
    }
}
