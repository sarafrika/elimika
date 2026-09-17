package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.TrainingRateFloorFlagsDTO;
import apps.sarafrika.elimika.course.model.TrainingRateCardHolder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.function.Predicate;

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

    /** True per cell when the rate is set and below {@code minimumTrainingFee}. */
    public static TrainingRateFloorFlagsDTO floorFlags(TrainingRateCardHolder holder, BigDecimal minimumTrainingFee) {
        BigDecimal floor = minimumTrainingFee == null ? BigDecimal.ZERO : minimumTrainingFee;
        Predicate<BigDecimal> below = rate -> rate != null && rate.compareTo(floor) < 0;
        return new TrainingRateFloorFlagsDTO(
                floor,
                below.test(holder.getPrivateOnlineHourlyRate()),
                below.test(holder.getPrivateInpersonHourlyRate()),
                below.test(holder.getGroupOnlineHourlyRate()),
                below.test(holder.getGroupInpersonHourlyRate()),
                below.test(holder.getPrivateOnlineSessionRate()),
                below.test(holder.getPrivateInpersonSessionRate()),
                below.test(holder.getGroupOnlineSessionRate()),
                below.test(holder.getGroupInpersonSessionRate()),
                below.test(holder.getPrivateOnlineDailyRate()),
                below.test(holder.getPrivateInpersonDailyRate()),
                below.test(holder.getGroupOnlineDailyRate()),
                below.test(holder.getGroupInpersonDailyRate())
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
