package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseTrainingApplicationFactory {

    public static CourseTrainingApplicationDTO toDTO(CourseTrainingApplication entity) {
        if (entity == null) {
            return null;
        }
        CourseTrainingRateCardDTO rateCard = new CourseTrainingRateCardDTO(
                entity.getRateCurrency(),
                entity.getPrivateOnlineHourlyRate(),
                entity.getPrivateInpersonHourlyRate(),
                entity.getGroupOnlineHourlyRate(),
                entity.getGroupInpersonHourlyRate(),
                entity.getPrivateOnlineSessionRate(),
                entity.getPrivateInpersonSessionRate(),
                entity.getGroupOnlineSessionRate(),
                entity.getGroupInpersonSessionRate(),
                entity.getPrivateOnlineDailyRate(),
                entity.getPrivateInpersonDailyRate(),
                entity.getGroupOnlineDailyRate(),
                entity.getGroupInpersonDailyRate()
        );

        return new CourseTrainingApplicationDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getApplicantType(),
                entity.getApplicantUuid(),
                entity.getStatus(),
                rateCard,
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
