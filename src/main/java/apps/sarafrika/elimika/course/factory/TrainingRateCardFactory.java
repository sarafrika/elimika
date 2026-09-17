package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.model.TrainingRateCardHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TrainingRateCardFactory {

    public static CourseTrainingRateCardDTO toDTO(TrainingRateCardHolder holder) {
        if (holder == null) {
            return null;
        }
        return new CourseTrainingRateCardDTO(
                holder.getRateCurrency(),
                holder.getPrivateOnlineHourlyRate(),
                holder.getPrivateInpersonHourlyRate(),
                holder.getGroupOnlineHourlyRate(),
                holder.getGroupInpersonHourlyRate(),
                holder.getPrivateOnlineSessionRate(),
                holder.getPrivateInpersonSessionRate(),
                holder.getGroupOnlineSessionRate(),
                holder.getGroupInpersonSessionRate(),
                holder.getPrivateOnlineDailyRate(),
                holder.getPrivateInpersonDailyRate(),
                holder.getGroupOnlineDailyRate(),
                holder.getGroupInpersonDailyRate()
        );
    }

    /** Overwrites every cell, so a rate absent from the card is cleared on the target. */
    public static void apply(TrainingRateCardHolder target, CourseTrainingRateCardDTO rateCard, String rateCurrency) {
        if (rateCard == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        target.setRateCurrency(rateCurrency);
        target.setPrivateOnlineHourlyRate(rateCard.privateOnlineHourlyRate());
        target.setPrivateInpersonHourlyRate(rateCard.privateInpersonHourlyRate());
        target.setGroupOnlineHourlyRate(rateCard.groupOnlineHourlyRate());
        target.setGroupInpersonHourlyRate(rateCard.groupInpersonHourlyRate());
        target.setPrivateOnlineSessionRate(rateCard.privateOnlineSessionRate());
        target.setPrivateInpersonSessionRate(rateCard.privateInpersonSessionRate());
        target.setGroupOnlineSessionRate(rateCard.groupOnlineSessionRate());
        target.setGroupInpersonSessionRate(rateCard.groupInpersonSessionRate());
        target.setPrivateOnlineDailyRate(rateCard.privateOnlineDailyRate());
        target.setPrivateInpersonDailyRate(rateCard.privateInpersonDailyRate());
        target.setGroupOnlineDailyRate(rateCard.groupOnlineDailyRate());
        target.setGroupInpersonDailyRate(rateCard.groupInpersonDailyRate());
    }
}
