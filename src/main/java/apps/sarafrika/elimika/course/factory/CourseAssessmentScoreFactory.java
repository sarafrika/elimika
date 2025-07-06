package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentScoreDTO;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentScoreFactory {

    // Convert CourseAssessmentScore entity to CourseAssessmentScoreDTO
    public static CourseAssessmentScoreDTO toDTO(CourseAssessmentScore courseAssessmentScore) {
        if (courseAssessmentScore == null) {
            return null;
        }
        return new CourseAssessmentScoreDTO(
                courseAssessmentScore.getUuid(),
                courseAssessmentScore.getEnrollmentUuid(),
                courseAssessmentScore.getAssessmentUuid(),
                courseAssessmentScore.getScore(),
                courseAssessmentScore.getMaxScore(),
                courseAssessmentScore.getPercentage(),
                courseAssessmentScore.getGradedAt(),
                courseAssessmentScore.getGradedByUuid(),
                courseAssessmentScore.getComments(),
                courseAssessmentScore.getCreatedDate(),
                courseAssessmentScore.getCreatedBy(),
                courseAssessmentScore.getLastModifiedDate(),
                courseAssessmentScore.getLastModifiedBy()
        );
    }

    // Convert CourseAssessmentScoreDTO to CourseAssessmentScore entity
    public static CourseAssessmentScore toEntity(CourseAssessmentScoreDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseAssessmentScore courseAssessmentScore = new CourseAssessmentScore();
        courseAssessmentScore.setUuid(dto.uuid());
        courseAssessmentScore.setEnrollmentUuid(dto.enrollmentUuid());
        courseAssessmentScore.setAssessmentUuid(dto.assessmentUuid());
        courseAssessmentScore.setScore(dto.score());
        courseAssessmentScore.setMaxScore(dto.maxScore());
        courseAssessmentScore.setPercentage(dto.percentage());
        courseAssessmentScore.setGradedAt(dto.gradedAt());
        courseAssessmentScore.setGradedByUuid(dto.gradedByUuid());
        courseAssessmentScore.setComments(dto.comments());
        courseAssessmentScore.setCreatedDate(dto.createdDate());
        courseAssessmentScore.setCreatedBy(dto.createdBy());
        courseAssessmentScore.setLastModifiedDate(dto.updatedDate());
        courseAssessmentScore.setLastModifiedBy(dto.updatedBy());
        return courseAssessmentScore;
    }
}