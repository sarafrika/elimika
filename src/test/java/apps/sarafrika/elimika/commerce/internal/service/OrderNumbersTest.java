package apps.sarafrika.elimika.commerce.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderNumbersTest {

    private final OrderNumbers orderNumbers = OrderNumbers.of(OrderNumbers.DEFAULT_ALPHABET);

    @Test
    void everySequenceValueGetsItsOwnShortReadableNumber() {
        Set<String> seen = new HashSet<>();
        for (long sequence = 1; sequence <= 200_000; sequence++) {
            String number = orderNumbers.format(sequence);
            assertThat(seen.add(number)).as("duplicate %s", number).isTrue();
            // Daraja caps the STK AccountReference at 12 characters.
            assertThat(number).hasSizeLessThanOrEqualTo(12).matches("ELM-[" + OrderNumbers.CHARACTERS + "]{6}");
        }
    }

    @Test
    void theSameSequenceAlwaysGivesTheSameNumber() {
        assertThat(orderNumbers.format(42)).isEqualTo(OrderNumbers.of(OrderNumbers.DEFAULT_ALPHABET).format(42));
    }

    @Test
    void anAlphabetThatIsNotAReorderingOfTheCharacterSetIsRefused() {
        assertThatThrownBy(() -> OrderNumbers.of("ABC")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderNumbers.of(OrderNumbers.CHARACTERS.replace('A', 'O')))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
