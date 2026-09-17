package apps.sarafrika.elimika.course.validation;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.util.enums.TrainingRateCell;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * A method is offered when any of its three bases is priced; an offered method prices all three at or above the minimum fee.
 */
@Component
public class CourseTrainingRateCardValidator {

    public void validateAgainstMinimum(CourseTrainingRateCardDTO rateCard, BigDecimal minimumTrainingFee) {
        if (rateCard == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        BigDecimal floor = minimumTrainingFee != null ? minimumTrainingFee : BigDecimal.ZERO;

        boolean offersAMethod = false;
        for (List<TrainingRateCell> method : TrainingRateCell.METHODS) {
            if (method.stream().allMatch(cell -> cell.read(rateCard) == null)) {
                continue;
            }
            offersAMethod = true;
            for (TrainingRateCell cell : method) {
                validateOfferedCell(cell, cell.read(rateCard), floor);
            }
        }
        if (!offersAMethod) {
            throw new IllegalArgumentException(
                    "At least one training method must be offered, with its hourly, session and daily rates");
        }
    }

    private void validateOfferedCell(TrainingRateCell cell, BigDecimal value, BigDecimal floor) {
        if (value == null) {
            throw new IllegalArgumentException(String.format(
                    "%s is required because %s training is offered; price all three bases or leave all three empty",
                    cell.fieldName(), cell.methodLabel()));
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(String.format(
                    "%s must be greater than zero; leave every %s rate empty when that method is not offered",
                    cell.fieldName(), cell.methodLabel()));
        }
        if (value.compareTo(floor) < 0) {
            throw new IllegalArgumentException(String.format(
                    "%s cannot be less than the minimum training fee of %s",
                    cell.fieldName(), floor.stripTrailingZeros().toPlainString()));
        }
    }
}
