package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentDTO;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentFactory {

    // Convert CourseAssessment entity to CourseAssessmentDTO
    public static CourseAssessmentDTO toDTO(CourseAssessment courseAssessment) {
        if (courseAssessment == null) {
            return null;
        }
        return new CourseAssessmentDTO(
                courseAssessment.getUuid(),
                courseAssessment.getCourseUuid(),
                courseAssessment.getAssessmentType(),
                courseAssessment.getTitle(),
                courseAssessment.getDescription(),
                courseAssessment.getWeightPercentage(),
                courseAssessment.getRubricUuid(),
                courseAssessment.getIsRequired(),
                courseAssessment.getCreatedDate(),
                courseAssessment.getCreatedBy(),
                courseAssessment.getLastModifiedDate(),
                courseAssessment.getLastModifiedBy()
        );
    }

    // Convert CourseAssessmentDTO to CourseAssessment entity
    public static CourseAssessment toEntity(CourseAssessmentDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseAssessment courseAssessment = new CourseAssessment();
        courseAssessment.setUuid(dto.uuid());
        courseAssessment.setCourseUuid(dto.courseUuid());
        courseAssessment.setAssessmentType(dto.assessmentType());
        courseAssessment.setTitle(dto.title());
        courseAssessment.setDescription(dto.description());
        courseAssessment.setWeightPercentage(dto.weightPercentage());
        courseAssessment.setRubricUuid(dto.rubricUuid());
        courseAssessment.setIsRequired(dto.isRequired());
        courseAssessment.setCreatedDate(dto.createdDate());
        courseAssessment.setCreatedBy(dto.createdBy());
        courseAssessment.setLastModifiedDate(dto.updatedDate());
        courseAssessment.setLastModifiedBy(dto.updatedBy());
        return courseAssessment;
    }
}