package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProgramTrainingApplicationFactory {

    public static ProgramTrainingApplicationDTO toDTO(ProgramTrainingApplication entity) {
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

        return new ProgramTrainingApplicationDTO(
                entity.getUuid(),
                entity.getProgramUuid(),
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
