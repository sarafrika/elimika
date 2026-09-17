package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationExtras;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProgramTrainingApplicationFactory {

    public static ProgramTrainingApplicationDTO toDTO(ProgramTrainingApplication entity, TrainingApplicationExtras extras) {
        if (entity == null) {
            return null;
        }

        CourseTrainingRateCardDTO rateCard = TrainingRateCardFactory.toDTO(entity);

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
                entity.getLastModifiedBy(),
                extras == null ? null : extras.pendingRateUpdateUuid(),
                extras == null ? null : extras.rateFloorFlags(),
                extras == null ? null : extras.firstOpenedAt()
        );
    }
}
