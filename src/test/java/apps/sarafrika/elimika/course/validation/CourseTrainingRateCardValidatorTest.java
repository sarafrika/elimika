package apps.sarafrika.elimika.course.validation;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.util.enums.TrainingRateCell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTrainingRateCardValidatorTest {

    private static final BigDecimal FLOOR = new BigDecimal("2000");

    private final CourseTrainingRateCardValidator validator = new CourseTrainingRateCardValidator();

    static Stream<Integer> methods() {
        return IntStream.range(0, TrainingRateCell.METHODS.size()).boxed();
    }

    @ParameterizedTest(name = "method {0} priced alone is a valid card")
    @MethodSource("methods")
    void eachMethodCanBeOfferedAlone(int method) {
        Map<TrainingRateCell, BigDecimal> cells = new EnumMap<>(TrainingRateCell.class);
        TrainingRateCell.METHODS.get(method).forEach(cell -> cells.put(cell, new BigDecimal("2500")));

        assertThatCode(() -> validator.validateAgainstMinimum(card(cells), FLOOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a card that offers no method is rejected")
    void aCardOfferingNothingIsRejected() {
        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(Map.of()), FLOOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one training method must be offered");
    }

    @Test
    @DisplayName("a not-offered method next to an offered one is left alone")
    void notOfferedRowsAreIgnored() {
        Map<TrainingRateCell, BigDecimal> cells = new EnumMap<>(TrainingRateCell.class);
        cells.put(TrainingRateCell.GROUP_ONLINE_HOURLY, new BigDecimal("2000"));
        cells.put(TrainingRateCell.GROUP_ONLINE_SESSION, new BigDecimal("3000"));
        cells.put(TrainingRateCell.GROUP_ONLINE_DAILY, new BigDecimal("9000"));

        assertThatCode(() -> validator.validateAgainstMinimum(card(cells), FLOOR)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} missing from its offered method is rejected")
    @EnumSource(TrainingRateCell.class)
    void aPartiallyPricedMethodIsRejected(TrainingRateCell missing) {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(missing, "2500");
        cells.remove(missing);

        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(cells), FLOOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missing.fieldName())
                .hasMessageContaining("is required");
    }

    @ParameterizedTest(name = "{0} below the minimum is rejected")
    @EnumSource(TrainingRateCell.class)
    void aRateBelowTheFloorIsRejected(TrainingRateCell low) {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(low, "2500");
        cells.put(low, new BigDecimal("1999.99"));

        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(cells), FLOOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(low.fieldName())
                .hasMessageContaining("minimum training fee");
    }

    @ParameterizedTest(name = "{0} at zero is rejected rather than read as not offered")
    @EnumSource(TrainingRateCell.class)
    void zeroIsNeverAPrice(TrainingRateCell zero) {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(zero, "2500");
        cells.put(zero, BigDecimal.ZERO);

        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(cells), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(zero.fieldName())
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("a method priced entirely at zero is still rejected")
    void aWholeMethodAtZeroIsRejected() {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(TrainingRateCell.PRIVATE_ONLINE_HOURLY, "0");

        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(cells), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("a negative rate is rejected")
    void aNegativeRateIsRejected() {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(TrainingRateCell.GROUP_INPERSON_DAILY, "2500");
        cells.put(TrainingRateCell.GROUP_INPERSON_DAILY, new BigDecimal("-1"));

        assertThatThrownBy(() -> validator.validateAgainstMinimum(card(cells), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("group_inperson_daily_rate");
    }

    @Test
    @DisplayName("a rate equal to the minimum is accepted")
    void theFloorItselfIsAccepted() {
        Map<TrainingRateCell, BigDecimal> cells = fullMethodOf(TrainingRateCell.PRIVATE_INPERSON_SESSION, "2000");

        assertThatCode(() -> validator.validateAgainstMinimum(card(cells), FLOOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a missing card is rejected")
    void aMissingCardIsRejected() {
        assertThatThrownBy(() -> validator.validateAgainstMinimum(null, FLOOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate card is required");
    }

    private static Map<TrainingRateCell, BigDecimal> fullMethodOf(TrainingRateCell cell, String amount) {
        List<TrainingRateCell> method = TrainingRateCell.METHODS.stream()
                .filter(candidate -> candidate.contains(cell))
                .findFirst()
                .orElseThrow();
        Map<TrainingRateCell, BigDecimal> cells = new EnumMap<>(TrainingRateCell.class);
        method.forEach(member -> cells.put(member, new BigDecimal(amount)));
        return cells;
    }

    private static CourseTrainingRateCardDTO card(Map<TrainingRateCell, BigDecimal> cells) {
        return new CourseTrainingRateCardDTO(
                "KES",
                cells.get(TrainingRateCell.PRIVATE_ONLINE_HOURLY),
                cells.get(TrainingRateCell.PRIVATE_INPERSON_HOURLY),
                cells.get(TrainingRateCell.GROUP_ONLINE_HOURLY),
                cells.get(TrainingRateCell.GROUP_INPERSON_HOURLY),
                cells.get(TrainingRateCell.PRIVATE_ONLINE_SESSION),
                cells.get(TrainingRateCell.PRIVATE_INPERSON_SESSION),
                cells.get(TrainingRateCell.GROUP_ONLINE_SESSION),
                cells.get(TrainingRateCell.GROUP_INPERSON_SESSION),
                cells.get(TrainingRateCell.PRIVATE_ONLINE_DAILY),
                cells.get(TrainingRateCell.PRIVATE_INPERSON_DAILY),
                cells.get(TrainingRateCell.GROUP_ONLINE_DAILY),
                cells.get(TrainingRateCell.GROUP_INPERSON_DAILY));
    }
}
