package apps.sarafrika.elimika.course.spi;

import java.math.BigDecimal;

/** One approved rate card cell: the rate in its stored basis and the card's currency. */
public record ApprovedTrainingRate(BigDecimal rate, String currency) {
}
