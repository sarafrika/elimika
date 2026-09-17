package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.TrainingRateUpdateDTO;
import apps.sarafrika.elimika.course.model.TrainingApplicationRecord;
import apps.sarafrika.elimika.course.model.TrainingRateUpdate;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TrainingRateUpdateFactory {

    public static TrainingRateUpdateDTO toDTO(TrainingRateUpdate update,
                                              TrainingApplicationRecord application,
                                              TrainingApplicationType type,
                                              UUID parentUuid,
                                              String applicantName) {
        return new TrainingRateUpdateDTO(
                update.getUuid(),
                update.getApplicationUuid(),
                type,
                type == TrainingApplicationType.COURSE ? parentUuid : null,
                type == TrainingApplicationType.PROGRAM ? parentUuid : null,
                application.getApplicantType(),
                application.getApplicantUuid(),
                applicantName,
                TrainingRateCardFactory.toDTO(application),
                TrainingRateCardFactory.toDTO(update),
                update.getNote(),
                update.getStatus(),
                update.getCreatedDate(),
                update.getReviewedBy(),
                update.getReviewedAt(),
                update.getReviewNotes()
        );
    }
}
