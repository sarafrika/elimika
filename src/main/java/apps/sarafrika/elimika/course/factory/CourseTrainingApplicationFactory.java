package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseTrainingApplicationFactory {

    public static CourseTrainingApplicationDTO toDTO(CourseTrainingApplication entity) {
        if (entity == null) {
            return null;
        }
        return new CourseTrainingApplicationDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getApplicantType(),
                entity.getApplicantUuid(),
                entity.getStatus(),
                entity.getRatePerHourPerHead(),
                entity.getRateCurrency(),
                entity.getApplicationNotes(),
                entity.getReviewNotes(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }
}
