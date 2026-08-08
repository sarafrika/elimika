package apps.sarafrika.elimika.course.validation;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates rate cards submitted with training applications to ensure every price point
 * respects the course minimum training fee (per learner per hour).
 */
@Component
public class CourseTrainingRateCardValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public void validateAgainstMinimum(CourseTrainingRateCardDTO rateCard, BigDecimal minimumTrainingFee) {
        if (rateCard == null) {
            throw new IllegalArgumentException("Rate card is required");
        }

        BigDecimal floor = minimumTrainingFee != null ? minimumTrainingFee : ZERO;

        // The course minimum is stated per learner per hour, so only the hourly rates are comparable
        // to it directly. Holding a per-session or per-day price to an hourly floor would be the same
        // unit confusion that let a class earn less than it cost.
        Map<String, BigDecimal> hourlyRates = new LinkedHashMap<>();
        hourlyRates.put("private_online_hourly_rate", rateCard.privateOnlineHourlyRate());
        hourlyRates.put("private_inperson_hourly_rate", rateCard.privateInpersonHourlyRate());
        hourlyRates.put("group_online_hourly_rate", rateCard.groupOnlineHourlyRate());
        hourlyRates.put("group_inperson_hourly_rate", rateCard.groupInpersonHourlyRate());
        hourlyRates.forEach((label, value) -> validateEntry(label, value, floor));

        // A session or a day is never shorter than an hour of teaching, so the hourly floor is the
        // weakest defensible bound for them: below it the rate cannot cover even a single hour.
        Map<String, BigDecimal> longerUnitRates = new LinkedHashMap<>();
        longerUnitRates.put("private_online_session_rate", rateCard.privateOnlineSessionRate());
        longerUnitRates.put("private_inperson_session_rate", rateCard.privateInpersonSessionRate());
        longerUnitRates.put("group_online_session_rate", rateCard.groupOnlineSessionRate());
        longerUnitRates.put("group_inperson_session_rate", rateCard.groupInpersonSessionRate());
        longerUnitRates.put("private_online_daily_rate", rateCard.privateOnlineDailyRate());
        longerUnitRates.put("private_inperson_daily_rate", rateCard.privateInpersonDailyRate());
        longerUnitRates.put("group_online_daily_rate", rateCard.groupOnlineDailyRate());
        longerUnitRates.put("group_inperson_daily_rate", rateCard.groupInpersonDailyRate());
        longerUnitRates.forEach((label, value) -> validateOptionalEntry(label, value, floor));
    }

    private void validateEntry(String label, BigDecimal value, BigDecimal floor) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
        if (value.compareTo(floor) < 0) {
            throw new IllegalArgumentException(String.format(
                    "%s %.4f cannot be less than the course minimum training fee %.2f per learner per hour",
                    label,
                    value,
                    floor
            ));
        }
    }

    /**
     * A basis the instructor has not priced yet is left alone; a card predating per-session and
     * per-day pricing stays valid for the hourly work it was approved for.
     */
    private void validateOptionalEntry(String label, BigDecimal value, BigDecimal floor) {
        if (value == null) {
            return;
        }
        validateEntry(label, value, floor);
    }
}
