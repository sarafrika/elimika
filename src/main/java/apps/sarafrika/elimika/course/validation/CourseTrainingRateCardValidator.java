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
        Map<String, BigDecimal> rateMap = new LinkedHashMap<>();
        rateMap.put("private_online_rate", rateCard.privateOnlineRate());
        rateMap.put("private_inperson_rate", rateCard.privateInpersonRate());
        rateMap.put("group_online_rate", rateCard.groupOnlineRate());
        rateMap.put("group_inperson_rate", rateCard.groupInpersonRate());

        rateMap.forEach((label, value) -> validateEntry(label, value, floor));
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
}
